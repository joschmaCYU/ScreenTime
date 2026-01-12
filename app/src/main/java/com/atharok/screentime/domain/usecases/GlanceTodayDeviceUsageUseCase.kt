package com.atharok.screentime.domain.usecases

import android.content.Context
import com.atharok.screentime.R
import com.atharok.screentime.common.utils.DateTimeUtils
import com.atharok.screentime.domain.entities.usage.GlanceTodayDeviceUsage
import com.atharok.screentime.domain.repositories.DeviceUsageRepository
import com.atharok.screentime.domain.resources.Resource

class GlanceTodayDeviceUsageUseCase(private val repository: DeviceUsageRepository) {

    // ---- Permission ----

    fun hasAppUsagePermission(): Boolean = repository.hasAppUsagePermission()

    // ---- Compute Device Usage ----

    private var calculationTimestamp = System.currentTimeMillis()

    private suspend fun calculateDeviceUsage(ignoredPackages: List<String>) {
        calculationTimestamp = System.currentTimeMillis()
        repository.calculateDeviceUsage(
            timeInterval = DateTimeUtils.computeInterval(calculationTimestamp, 1),
            ignoredPackages = ignoredPackages
        )
    }

    suspend fun calculateTodayDeviceUsage(
        ignoredPackages: List<String>,
        context: Context
    ): Resource<GlanceTodayDeviceUsage> {
        return if(!hasAppUsagePermission()) {
            Resource.failure(
                throwable = Throwable(
                    message = context.getString(R.string.access_to_usage_data_message)
                )
            )
        } else {
            try {
                calculateDeviceUsage(ignoredPackages)

                val dayTimestamp = DateTimeUtils.truncateToDay(calculationTimestamp)
                val usage = repository.getGlanceTodayDeviceUsage(
                    timeInterval = Pair(
                        first = dayTimestamp,
                        second = calculationTimestamp
                    ),
                    dayTimestamp = dayTimestamp
                )

                Resource.success(usage)
            } catch (e: Exception) {
                Resource.failure(e)
            }
        }
    }
}