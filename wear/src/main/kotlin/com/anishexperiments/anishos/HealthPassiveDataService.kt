package com.anishexperiments.anishos

import android.content.Context
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.HeartRateAccuracy
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

class HealthPassiveDataService : PassiveListenerService() {
    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return

        val heartRate = dataPoints.getData(DataType.HEART_RATE_BPM)
            .filter { it.value > 0.0 }
            .filter {
                val accuracy = it.accuracy as? HeartRateAccuracy
                accuracy == null ||
                    accuracy.sensorStatus == HeartRateAccuracy.SensorStatus.ACCURACY_HIGH ||
                    accuracy.sensorStatus == HeartRateAccuracy.SensorStatus.ACCURACY_MEDIUM
            }
            .maxByOrNull { it.timeDurationFromBoot }?.value
        val steps = dataPoints.getData(DataType.STEPS_DAILY)
            .maxByOrNull { it.endDurationFromBoot }?.value
        val calories = dataPoints.getData(DataType.CALORIES_DAILY)
            .maxByOrNull { it.endDurationFromBoot }?.value

        val now = System.currentTimeMillis()
        val editor = prefs.edit()
        if (heartRate != null) editor.putFloat(KEY_HEART_RATE, heartRate.toFloat())
        if (steps != null) editor.putLong(KEY_STEPS, steps)
        if (calories != null) editor.putFloat(KEY_CALORIES, calories.toFloat())
        editor.putLong(KEY_UPDATED_AT, now).apply()

        val linkPrefs = getSharedPreferences(WearDataLayerService.PREFS, Context.MODE_PRIVATE)
        if (!linkPrefs.getBoolean(WearDataLayerService.KEY_LINKED, false)) return
        val phoneNode = linkPrefs.getString(WearDataLayerService.KEY_LINKED_NODE, null)
            ?.takeIf { it.isNotBlank() } ?: return

        val payload = JSONObject()
            .put("heartRateBpm", heartRate)
            .put("stepsToday", steps)
            .put("caloriesTodayKcal", calories)
            .put("recordedAt", now)
            .put("source", "wear_health_services")
            .toString().toByteArray(Charsets.UTF_8)
        Wearable.getMessageClient(this).sendMessage(phoneNode, HEALTH_METRICS_PATH, payload)
    }

    companion object {
        const val PREFS = "anishos_health"
        const val KEY_ENABLED = "health_enabled"
        const val KEY_HEART_RATE = "heart_rate_bpm"
        const val KEY_STEPS = "steps_today"
        const val KEY_CALORIES = "calories_today_kcal"
        const val KEY_UPDATED_AT = "updated_at"
        const val HEALTH_METRICS_PATH = "/anishos/health/metrics"
    }
}
