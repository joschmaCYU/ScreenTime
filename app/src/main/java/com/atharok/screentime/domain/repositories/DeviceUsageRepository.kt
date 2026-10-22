package com.atharok.screentime.domain.repositories

import com.atharok.screentime.domain.entities.usage.AllDeviceUsage
import com.atharok.screentime.domain.entities.usage.DailyDeviceUsage
import com.atharok.screentime.domain.entities.usage.MonthlyDeviceUsage
import com.atharok.screentime.domain.entities.usage.WeeklyDeviceUsage
import com.atharok.screentime.domain.entities.usage.YearlyDeviceUsage

interface DeviceUsageRepository {

    // Permission

    fun hasAppUsagePermission(): Boolean

    // Calculate

    suspend fun calculateDeviceUsage(
        timeInterval: Pair<Long, Long>,
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    )

    suspend fun getWeekDeviceUsage(
        timeInterval: Pair<Long, Long>
    ): WeeklyDeviceUsage

    suspend fun getDayDeviceUsage(
        timeInterval: Pair<Long, Long>,
        dayTimestamp: Long
    ): DailyDeviceUsage

    suspend fun getMonthDeviceUsage(
        timeInterval: Pair<Long, Long>,
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ): MonthlyDeviceUsage

    suspend fun getYearDeviceUsage(
        timeInterval: Pair<Long, Long>,
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ): YearlyDeviceUsage

    suspend fun getAllDeviceUsage(
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ): AllDeviceUsage
}