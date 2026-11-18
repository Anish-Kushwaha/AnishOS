package com.anishexperiments.anishos

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectSleepRepository(private val context: Context) {
    fun sdkAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE) == HealthConnectClient.SDK_AVAILABLE

    fun readRecentSleep(): JSONArray = runBlocking {
        val client = HealthConnectClient.getOrCreate(context)
        val end = Instant.now()
        val start = end.minus(14, ChronoUnit.DAYS)
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                pageSize = 100
            )
        )
        JSONArray().apply {
            response.records.sortedByDescending { it.endTime }.forEach { record ->
                put(
                    JSONObject()
                        .put("startTime", record.startTime.toString())
                        .put("endTime", record.endTime.toString())
                        .put("durationMinutes", ChronoUnit.MINUTES.between(record.startTime, record.endTime))
                        .put("title", record.title ?: "Sleep session")
                        .put("stageCount", record.stages.size)
                )
            }
        }
    }

    companion object {
        const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
    }
}
