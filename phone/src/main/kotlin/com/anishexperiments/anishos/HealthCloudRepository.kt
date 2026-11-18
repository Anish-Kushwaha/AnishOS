package com.anishexperiments.anishos

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.anishexperiments.anishos.phone.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Client-side encryption for cloud health batches.
 * The AES key is device-bound in Android Keystore; Supabase receives ciphertext only.
 */
class HealthCloudRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val alias = "com.anishexperiments.anishos.health.v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun request(path: String, method: String, token: String, body: JSONObject? = null, prefer: String? = null): String {
        val connection = (URL(BuildConfig.SUPABASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            if (!prefer.isNullOrBlank()) setRequestProperty("Prefer", prefer)
            if (body != null) doOutput = true
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException("Health cloud request failed (HTTP $status): " + response.take(240))
            return response
        } finally {
            connection.disconnect()
        }
    }

    fun grantConsent(session: AnishSession) {
        val valid = PhoneAuthRepository(SecureSessionStore(context)).currentSession()
            ?: throw IllegalStateException("Sign in again before enabling cloud health sync.")
        request(
            "/rest/v1/health_sync_consents?on_conflict=user_id",
            "POST",
            valid.accessToken,
            JSONObject().put("user_id", valid.userId)
                .put("consent_version", CONSENT_VERSION)
                .put("revoked_at", JSONObject.NULL),
            "resolution=merge-duplicates,return=minimal"
        )
        prefs.edit().putBoolean(KEY_CONSENT, true).apply()
    }

    fun revokeConsentAndDelete(session: AnishSession) {
        val valid = PhoneAuthRepository(SecureSessionStore(context)).currentSession()
            ?: throw IllegalStateException("Sign in again before changing cloud health consent.")
        val now = java.time.Instant.now().toString()
        request(
            "/rest/v1/health_sync_consents?user_id=eq.${valid.userId}",
            "PATCH",
            valid.accessToken,
            JSONObject().put("revoked_at", now).put("updated_at", now),
            "return=minimal"
        )
        request(
            "/rest/v1/encrypted_health_batches?user_id=eq.${valid.userId}",
            "DELETE",
            valid.accessToken,
            prefer = "return=minimal"
        )
        prefs.edit().putBoolean(KEY_CONSENT, false).apply()
    }

    fun syncEncrypted(session: AnishSession, payload: String) {
        if (!prefs.getBoolean(KEY_CONSENT, false)) return
        val valid = PhoneAuthRepository(SecureSessionStore(context)).currentSession()
            ?: throw IllegalStateException("Sign in again to sync health data.")
        val consent = request(
            "/rest/v1/health_sync_consents?user_id=eq.${valid.userId}&select=revoked_at",
            "GET",
            valid.accessToken
        )
        val rows = org.json.JSONArray(consent)
        if (rows.length() == 0 || !rows.getJSONObject(0).isNull("revoked_at")) {
            prefs.edit().putBoolean(KEY_CONSENT, false).apply()
            return
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val envelope = JSONObject(payload).apply { if (optString("batchId").isBlank()) put("batchId", java.util.UUID.randomUUID().toString()) }
        val encrypted = cipher.doFinal(envelope.toString().toByteArray(Charsets.UTF_8))
        val nonce = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val ciphertext = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        val recordedAt = try {
            envelope.optLong("recordedAt", System.currentTimeMillis())
        } catch (_: Exception) { System.currentTimeMillis() }

        request(
            "/rest/v1/encrypted_health_batches?on_conflict=user_id,batch_id",
            "POST",
            valid.accessToken,
            JSONObject()
                .put("user_id", valid.userId)
                .put("batch_id", envelope.getString("batchId"))
                .put("schema_version", 1)
                .put("algorithm", "AES-256-GCM")
                .put("key_version", "android-keystore-v1")
                .put("nonce_b64", nonce)
                .put("ciphertext_b64", ciphertext)
                .put("recorded_at", java.time.Instant.ofEpochMilli(recordedAt).toString()),
            "resolution=ignore-duplicates,return=minimal"
        )
        prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
    }

    fun hasConsent(): Boolean = prefs.getBoolean(KEY_CONSENT, false)
    fun lastSyncAt(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    companion object {
        const val PREFS = "anishos_health_cloud"
        const val KEY_CONSENT = "cloud_consent"
        const val KEY_LAST_SYNC = "last_sync_at"
        const val CONSENT_VERSION = "2026-11-v1"
    }
}

object HealthCloudSync {
    private const val UNIQUE_WORK = "anishos-encrypted-health-sync"

    fun enqueue(context: Context, payload: String) {
        val app = context.applicationContext
        if (!HealthCloudRepository(app).hasConsent()) return
        synchronized(this) {
            val prefs = app.getSharedPreferences(HealthCloudRepository.PREFS, Context.MODE_PRIVATE)
            val queue = try { org.json.JSONArray(prefs.getString("pending_queue", "[]")) } catch (_: Exception) { org.json.JSONArray() }
            val envelope = JSONObject(payload).apply {
                if (optString("batchId").isBlank()) put("batchId", java.util.UUID.randomUUID().toString())
            }
            if (queue.length() >= 250) queue.remove(0)
            queue.put(envelope)
            prefs.edit().putString("pending_queue", queue.toString()).apply()
        }
        schedule(app)
    }

    fun retry(context: Context) {
        val app = context.applicationContext
        val queue = app.getSharedPreferences(HealthCloudRepository.PREFS, Context.MODE_PRIVATE)
            .getString("pending_queue", "[]") ?: "[]"
        if (queue == "[]") return
        schedule(app)
    }

    private fun schedule(context: Context) {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
        val request = androidx.work.OneTimeWorkRequestBuilder<HealthCloudSyncWorker>()
            .setConstraints(constraints)
            .build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK, androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE, request
        )
    }
}

class HealthCloudSyncWorker(context: Context, params: androidx.work.WorkerParameters) :
    androidx.work.Worker(context, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(HealthCloudRepository.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(HealthCloudRepository.KEY_CONSENT, false)) return Result.success()
        return try {
            val session = PhoneAuthRepository(SecureSessionStore(applicationContext)).currentSession()
                ?: return Result.retry()
            while (true) {
                val queue = try { org.json.JSONArray(prefs.getString("pending_queue", "[]")) } catch (_: Exception) { org.json.JSONArray() }
                if (queue.length() == 0) break
                HealthCloudRepository(applicationContext).syncEncrypted(session, queue.getJSONObject(0).toString())
                val remaining = org.json.JSONArray()
                for (index in 1 until queue.length()) remaining.put(queue.get(index))
                prefs.edit().putString("pending_queue", remaining.toString()).apply()
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
