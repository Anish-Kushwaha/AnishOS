package com.anishexperiments.anishos

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class AnishSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val userId: String,
    val email: String
)

class SecureSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("anishos_auth_v1", Context.MODE_PRIVATE)
    private val alias = "com.anishexperiments.anishos.session.v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun save(session: AnishSession) {
        val payload = JSONObject()
            .put("access", session.accessToken)
            .put("refresh", session.refreshToken)
            .put("expires", session.expiresAt)
            .put("user", session.userId)
            .put("email", session.email)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(payload)
        val packed = cipher.iv + encrypted
        prefs.edit().putString("blob", Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    fun load(): AnishSession? {
        return try {
            val encoded = prefs.getString("blob", null) ?: return null
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            if (packed.size <= 12) return null
            val iv = packed.copyOfRange(0, 12)
            val ciphertext = packed.copyOfRange(12, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            val json = JSONObject(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
            AnishSession(
                accessToken = json.getString("access"),
                refreshToken = json.getString("refresh"),
                expiresAt = json.getLong("expires"),
                userId = json.getString("user"),
                email = json.optString("email")
            )
        } catch (_: Exception) {
            clear()
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

class PhoneAuthRepository(private val store: SecureSessionStore) {
    private fun request(
        path: String,
        method: String,
        body: JSONObject? = null,
        accessToken: String? = null,
        prefer: String? = null
    ): String {
        val connection = (URL(BuildConfig.SUPABASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-Client-Info", "anishos-phone/0.2.0")
            if (!accessToken.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $accessToken")
            if (!prefer.isNullOrBlank()) setRequestProperty("Prefer", prefer)
            if (body != null) doOutput = true
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = try {
                    val error = JSONObject(response)
                    error.optString("msg").ifBlank {
                        error.optString("message").ifBlank { error.optString("error_description") }
                    }
                } catch (_: Exception) { "" }
                throw IllegalStateException(message.ifBlank { "Request failed (HTTP $status)." })
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun sessionFrom(json: JSONObject, previous: AnishSession? = null): AnishSession {
        val user = json.optJSONObject("user")
        val userId = user?.optString("id")?.takeIf { it.isNotBlank() } ?: previous?.userId
            ?: throw IllegalStateException("Authentication response did not include a user ID.")
        val email = user?.optString("email")?.takeIf { it.isNotBlank() } ?: previous?.email.orEmpty()
        val expiresIn = json.optLong("expires_in", 3600L)
        return AnishSession(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            expiresAt = System.currentTimeMillis() + expiresIn * 1000L,
            userId = userId,
            email = email
        )
    }

    fun signIn(email: String, password: String): AnishSession {
        val response = request(
            "/auth/v1/token?grant_type=password",
            "POST",
            JSONObject().put("email", email).put("password", password)
        )
        return sessionFrom(JSONObject(response)).also(store::save)
    }

    fun signUp(email: String, password: String): Boolean {
        val response = JSONObject(
            request(
                "/auth/v1/signup",
                "POST",
                JSONObject().put("email", email).put("password", password)
            )
        )
        if (!response.has("access_token") || response.optString("access_token").isBlank()) return false
        store.save(sessionFrom(response))
        return true
    }

    fun currentSession(): AnishSession? {
        val session = store.load() ?: return null
        if (session.expiresAt > System.currentTimeMillis() + 60_000L) return session
        return try {
            val refreshed = JSONObject(
                request(
                    "/auth/v1/token?grant_type=refresh_token",
                    "POST",
                    JSONObject().put("refresh_token", session.refreshToken)
                )
            )
            sessionFrom(refreshed, session).also(store::save)
        } catch (_: Exception) {
            store.clear()
            null
        }
    }

    fun registerPendingWatch(session: AnishSession, name: String, model: String) {
        val validSession = currentSession() ?: throw IllegalStateException("Your session expired. Sign in again.")
        request(
            "/rest/v1/paired_devices",
            "POST",
            JSONObject()
                .put("user_id", validSession.userId)
                .put("device_name", name.take(64))
                .put("device_model", model.take(128))
                .put("device_type", "wear_os")
                .put("status", "pending"),
            validSession.accessToken,
            "return=minimal"
        )
    }

    fun signOut() {
        val session = store.load()
        try {
            if (session != null) request("/auth/v1/logout", "POST", accessToken = session.accessToken)
        } finally {
            store.clear()
        }
    }
}
