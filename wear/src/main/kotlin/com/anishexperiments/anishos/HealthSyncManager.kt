package com.anishexperiments.anishos

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.common.util.concurrent.ListenableFuture
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import java.util.concurrent.Executors

object HealthSyncManager {
    const val PERMISSION_REQUEST = 420
    private val executor = Executors.newSingleThreadExecutor()

    fun enable(activity: Activity, onStatus: (String) -> Unit) {
        val foregroundPermissions = mutableListOf(Manifest.permission.BODY_SENSORS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            foregroundPermissions += Manifest.permission.ACTIVITY_RECOGNITION
        }
        val missingForeground = foregroundPermissions.filter {
            activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingForeground.isNotEmpty()) {
            activity.requestPermissions(missingForeground.toTypedArray(), PERMISSION_REQUEST)
            onStatus("Grant the requested sensor permissions, then tap Enable again.")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            activity.checkSelfPermission(Manifest.permission.BODY_SENSORS_BACKGROUND) != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS_BACKGROUND), PERMISSION_REQUEST)
            onStatus("Background heart-rate access is required for all-day passive monitoring. Choose All the time in sensor access settings, then tap Enable again.")
            return
        }
        register(activity, onStatus)
    }

    private fun register(context: Context, onStatus: (String) -> Unit) {
        val client = HealthServices.getClient(context).passiveMonitoringClient
        val capabilitiesFuture = client.getCapabilitiesAsync()
        capabilitiesFuture.addListener({
            try {
                val capabilities = capabilitiesFuture.get()
                val wanted = setOf(DataType.HEART_RATE_BPM, DataType.STEPS_DAILY, DataType.CALORIES_DAILY)
                val supported = wanted.intersect(capabilities.getSupportedDataTypesPassiveMonitoring())
                if (supported.isEmpty()) {
                    onStatus("This watch does not expose compatible passive health metrics.")
                    return@addListener
                }
                val config = PassiveListenerConfig.builder().setDataTypes(supported).build()
                val registerFuture = client.setPassiveListenerServiceAsync(HealthPassiveDataService::class.java, config)
                registerFuture.addListener({
                    try {
                        registerFuture.get()
                        context.getSharedPreferences(HealthPassiveDataService.PREFS, Context.MODE_PRIVATE)
                            .edit().putBoolean(HealthPassiveDataService.KEY_ENABLED, true).apply()
                        onStatus("Health monitoring enabled. Health Services may batch updates to conserve battery.")
                    } catch (error: Exception) {
                        onStatus(error.cause?.localizedMessage ?: error.localizedMessage ?: "Health Services could not start.")
                    }
                }, executor)
            } catch (error: Exception) {
                onStatus(error.cause?.localizedMessage ?: error.localizedMessage ?: "Health Services capabilities are unavailable.")
            }
        }, executor)
    }

    fun disable(context: Context, onStatus: (String) -> Unit) {
        val future = HealthServices.getClient(context).passiveMonitoringClient.clearPassiveListenerServiceAsync()
        future.addListener({
            try {
                future.get()
                context.getSharedPreferences(HealthPassiveDataService.PREFS, Context.MODE_PRIVATE)
                    .edit().putBoolean(HealthPassiveDataService.KEY_ENABLED, false).apply()
                onStatus("Health monitoring disabled.")
            } catch (error: Exception) {
                onStatus(error.cause?.localizedMessage ?: error.localizedMessage ?: "Could not disable health monitoring.")
            }
        }, executor)
    }
}
