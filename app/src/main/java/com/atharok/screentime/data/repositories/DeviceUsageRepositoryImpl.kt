package com.atharok.screentime.data.repositories

import android.content.Context
import com.atharok.screentime.common.extensions.getApplicationIconSafety
import com.atharok.screentime.common.extensions.getApplicationName
import com.atharok.screentime.common.utils.DateTimeUtils
import com.atharok.screentime.data.statsTracker.UsageStatsTracker
import com.atharok.screentime.domain.entities.usage.AllAppUsage
import com.atharok.screentime.domain.entities.usage.AllDeviceUsage
import com.atharok.screentime.domain.entities.usage.DailyAppUsage
import com.atharok.screentime.domain.entities.usage.DailyDeviceUsage
import com.atharok.screentime.domain.entities.usage.MonthlyAppUsage
import com.atharok.screentime.domain.entities.usage.MonthlyDeviceUsage
import com.atharok.screentime.domain.entities.usage.WeeklyAppUsage
import com.atharok.screentime.domain.entities.usage.WeeklyDeviceUsage
import com.atharok.screentime.domain.entities.usage.YearlyAppUsage
import com.atharok.screentime.domain.entities.usage.YearlyDeviceUsage
import com.atharok.screentime.domain.repositories.DeviceUsageRepository

class DeviceUsageRepositoryImpl(
    private val context: Context,
    private val tracker: UsageStatsTracker
): DeviceUsageRepository {

    // Permission

    override fun hasAppUsagePermission(): Boolean = tracker.hasAppUsagePermission()

    // Calculate

    private var deviceUsageCacheByDayAndHour: Map<String, Map<Long, LongArray>>? = null
    private var totalDeviceUsageCacheByDayAndHour: Map<Long, LongArray> = mapOf()
    private var dateOfLastRefresh: String = ""

    override suspend fun calculateDeviceUsage(
        timeInterval: Pair<Long, Long>,
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ) {
        val usageIntervalsByPackage: Map<String, List<Pair<Long, Long>>> = tracker
            .calculateUsageIntervalsByPackage(
                startTimestamp = timeInterval.first,
                endTimestamp = timeInterval.second,
                ignoreSystemApps = ignoreSystemApps,
                ignoredPackages = ignoredPackages
            )

        deviceUsageCacheByDayAndHour = tracker.getDurationPerPackageByDayAndHour(
            startTimestamp = timeInterval.first,
            endTimestamp = timeInterval.second,
            usageIntervalsByPackage = usageIntervalsByPackage
        )

        totalDeviceUsageCacheByDayAndHour = tracker.getTotalDurationByDayAndHour(
            startTimestamp = timeInterval.first,
            endTimestamp = timeInterval.second,
            usageIntervalsByPackage = usageIntervalsByPackage
        )

        dateOfLastRefresh = DateTimeUtils.formatToFullDateTime(timeInterval.second)
    }

    override suspend fun getWeekDeviceUsage(
        timeInterval: Pair<Long, Long>
    ): WeeklyDeviceUsage {

        val deviceUsageCacheByDayAndHourNotNull = deviceUsageCacheByDayAndHour
            ?: throw Exception("DeviceUsageRepositoryImpl.calculateDeviceUsage() not called.")

        return WeeklyDeviceUsage(
            usages = deviceUsageCacheByDayAndHourNotNull.mapValues { (key: String, value: Map<Long, LongArray>) ->
                WeeklyAppUsage(
                    packageName = key,
                    appName = context.packageManager.getApplicationName(key) ?: "",
                    appIcon = context.packageManager.getApplicationIconSafety(key),
                    durations = value
                        .filterKeys { timestamp ->
                            timestamp in timeInterval.first..timeInterval.second
                        }
                )
            },
            timeInterval = timeInterval,
            dateOfLastRefresh = dateOfLastRefresh,
            totalDeviceUsageByDayAndHour = totalDeviceUsageCacheByDayAndHour.filterKeys { timestamp ->
                timestamp in timeInterval.first..timeInterval.second
            }
        )
    }

    override suspend fun getDayDeviceUsage(
        timeInterval: Pair<Long, Long>,
        dayTimestamp: Long
    ): DailyDeviceUsage {

        val deviceUsageCacheByDayAndHourNotNull = deviceUsageCacheByDayAndHour
            ?: throw Exception("DeviceUsageRepositoryImpl.calculateDeviceUsage() not called.")

        return DailyDeviceUsage(
            usages = deviceUsageCacheByDayAndHourNotNull.mapValues { (key: String, value: Map<Long, LongArray>) ->
                DailyAppUsage(
                    packageName = key,
                    appName = context.packageManager.getApplicationName(key) ?: "",
                    appIcon = context.packageManager.getApplicationIconSafety(key),
                    durations = value[dayTimestamp] ?: LongArray(24)
                )
            },
            timeInterval = timeInterval,
            dateOfLastRefresh = dateOfLastRefresh,
            totalDeviceUsageByHour = totalDeviceUsageCacheByDayAndHour[dayTimestamp] ?: LongArray(24)
        )
    }

    override suspend fun getMonthDeviceUsage(
        timeInterval: Pair<Long, Long>,
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ): MonthlyDeviceUsage {
        val (durationsByPackage, totalDurationByDay) = tracker.getMonthlyUsage(
            startTimestamp = timeInterval.first,
            endTimestamp = timeInterval.second,
            ignoreSystemApps = ignoreSystemApps,
            ignoredPackages = ignoredPackages
        )

        return MonthlyDeviceUsage(
            usages = durationsByPackage.mapValues { (key: String, value: Map<Long, Long>) ->
                MonthlyAppUsage(
                    packageName = key,
                    appName = context.packageManager.getApplicationName(key) ?: "",
                    appIcon = context.packageManager.getApplicationIconSafety(key),
                    durations = value
                )
            },
            timeInterval = timeInterval,
            dateOfLastRefresh = dateOfLastRefresh.ifEmpty { DateTimeUtils.formatToFullDateTime(System.currentTimeMillis()) },
            totalDeviceUsageByDay = totalDurationByDay
        )
    }

    override suspend fun getYearDeviceUsage(
        timeInterval: Pair<Long, Long>,
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ): YearlyDeviceUsage {
        val (durationsByPackage, totalDurationByMonth) = tracker.getYearlyUsage(
            startTimestamp = timeInterval.first,
            endTimestamp = timeInterval.second,
            ignoreSystemApps = ignoreSystemApps,
            ignoredPackages = ignoredPackages
        )

        return YearlyDeviceUsage(
            usages = durationsByPackage.mapValues { (key: String, value: Map<Long, Long>) ->
                YearlyAppUsage(
                    packageName = key,
                    appName = context.packageManager.getApplicationName(key) ?: "",
                    appIcon = context.packageManager.getApplicationIconSafety(key),
                    durations = value
                )
            },
            timeInterval = timeInterval,
            dateOfLastRefresh = dateOfLastRefresh.ifEmpty { DateTimeUtils.formatToFullDateTime(System.currentTimeMillis()) },
            totalDeviceUsageByMonth = totalDurationByMonth
        )
    }

    override suspend fun getAllDeviceUsage(
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ): AllDeviceUsage {
        val (durationsByPackage, totalDurationByYear) = tracker.getAllUsage(
            ignoreSystemApps = ignoreSystemApps,
            ignoredPackages = ignoredPackages
        )

        val startTimestamp = totalDurationByYear.keys.minOrNull() ?: 0L
        val endTimestamp = System.currentTimeMillis()

        return AllDeviceUsage(
            usages = durationsByPackage.mapValues { (key: String, value: Map<Long, Long>) ->
                AllAppUsage(
                    packageName = key,
                    appName = context.packageManager.getApplicationName(key) ?: "",
                    appIcon = context.packageManager.getApplicationIconSafety(key),
                    durations = value
                )
            },
            timeInterval = Pair(startTimestamp, endTimestamp),
            dateOfLastRefresh = dateOfLastRefresh.ifEmpty { DateTimeUtils.formatToFullDateTime(endTimestamp) },
            totalDeviceUsageByYear = totalDurationByYear
        )
    }
}