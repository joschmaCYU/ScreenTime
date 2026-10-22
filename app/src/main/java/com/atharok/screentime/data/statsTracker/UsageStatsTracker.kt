package com.atharok.screentime.data.statsTracker

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.atharok.screentime.common.utils.DateTimeUtils
import java.util.Calendar
import kotlin.math.abs

class UsageStatsTracker(
    private val context: Context,
    private val usageStatsManager: UsageStatsManager,
    private val appOps: AppOpsManager
) {

    // ---- Permission ----

    fun hasAppUsagePermission(): Boolean {
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // ---- Usage calculation ----

    private val launchableApp: Set<String> by lazy {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        resolveInfos.map { it.activityInfo.packageName }.toSet()
    }

    private data class ResumedPackage(
        val packageName: String,
        val timestamp: Long,
    )

    /**
     * Calculates the different intervals for each app.
     */
    suspend fun calculateUsageIntervalsByPackage(
        startTimestamp: Long,
        endTimestamp: Long,
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ): Map<String, List<Pair<Long, Long>>> {
        val usageIntervals = mutableMapOf<String, MutableList<Pair<Long, Long>>>()

        usageStatsManager.queryEvents(startTimestamp, endTimestamp)?.let { usageEvents: UsageEvents ->

            val resumedPackages = mutableMapOf<String, ResumedPackage>()
            //val isFirstResumedCalled = mutableMapOf<String, Boolean>()

            while (usageEvents.hasNextEvent()) {
                val event = UsageEvents.Event()

                if(usageEvents.getNextEvent(event)) {

                    if(ignoreSystemApps && event.packageName !in launchableApp) continue

                    if(ignoredPackages.contains(event.packageName)) continue

                    event.packageName?.let { eventPackageName: String ->
                        val packageClassName: String = event.className ?: eventPackageName

                        when(event.eventType) {

                            UsageEvents.Event.ACTIVITY_RESUMED -> {
                                resumedPackages[packageClassName] = ResumedPackage(
                                    packageName = eventPackageName,
                                    timestamp = event.timeStamp
                                )
                                //isFirstResumedCalled[packageClassName] = true
                            }

                            UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                                // If the app is in the foreground.
                                resumedPackages[packageClassName]?.let { resumedPackage ->
                                    usageIntervals
                                        .getOrPut(eventPackageName) { mutableListOf() }
                                        .add(resumedPackage.timestamp to event.timeStamp)
                                    resumedPackages.remove(packageClassName)
                                }/* ?: run {
                                    // If the first RESUMED state was not called, we assume it
                                    // occurred before the start timestamp passed as a function
                                    // parameter. Therefore we add the interval between the provided
                                    // start timestamp and the current PAUSED/STOPPED timestamp.
                                    if(isFirstResumedCalled[packageClassName] != true) {
                                        usageIntervals
                                            .getOrPut(eventPackageName) { mutableListOf() }
                                            .add(startTimestamp to event.timeStamp)
                                        isFirstResumedCalled[packageClassName] = true
                                    }
                                }*/ // -> May cause errors, so we remove it
                            }

                        }

                    }
                }
            }

            // If the last retrieved event was in the RESUMED state, we add the interval between
            // the timestamp of that RESUMED event and the end timestamp provided as a function
            // parameter.
            resumedPackages.maxByOrNull {
                it.value.timestamp
            }?.value?.let { lastResumedPackage: ResumedPackage ->
                usageIntervals
                    .getOrPut(lastResumedPackage.packageName) { mutableListOf() }
                    .add(lastResumedPackage.timestamp to endTimestamp)
            }
        }

        return usageIntervals
    }

    /**
     * Calculates screen time for each app by day and hour.
     */
    suspend fun getDurationPerPackageByDayAndHour(
        startTimestamp: Long,
        endTimestamp: Long,
        usageIntervalsByPackage: Map<String, List<Pair<Long, Long>>>
    ): Map<String, Map<Long, LongArray>> {
        val durationPerPackageByDayAndHour = mutableMapOf<String, Map<Long, LongArray>>()

        usageIntervalsByPackage.forEach { (packageName: String, intervals: List<Pair<Long, Long>>) ->
            durationPerPackageByDayAndHour[packageName] = generateDurationMapByDayAndHour(
                intervals = intervals,
                startTimestamp = startTimestamp,
                endTimestamp = endTimestamp
            )
        }

        return durationPerPackageByDayAndHour
    }

    /**
     * Calculates total screen time independently of individual apps. For this calculation,
     * overlapping intervals are merged.
     */
    suspend fun getTotalDurationByDayAndHour(
        startTimestamp: Long,
        endTimestamp: Long,
        usageIntervalsByPackage: Map<String, List<Pair<Long, Long>>>
    ): Map<Long, LongArray> {
        val intervals: List<Pair<Long, Long>> = mergeOverlappingIntervals(
            intervals = usageIntervalsByPackage.values.flatMap { it }
        )

        val totalDurationPerByDayAndHour = generateDurationMapByDayAndHour(
            intervals = intervals,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp
        )

        return totalDurationPerByDayAndHour
    }

    /**
     * Merge the overlapping intervals.
     * Handles the case where multiple apps are brought to the foreground at the same time
     * (split-screen mode). To do this, overlapping intervals are merged.
     */
    private fun mergeOverlappingIntervals(intervals: List<Pair<Long, Long>>): List<Pair<Long, Long>> {
        if (intervals.isEmpty()) return emptyList()

        // Sorts the intervals
        val sortedIntervals = intervals.sortedBy { it.first }

        // Initializes the result list with the first interval.
        val mergedIntervals = mutableListOf<Pair<Long, Long>>()
        var currentInterval = sortedIntervals[0]

        for (i in 1 until sortedIntervals.size) {
            val nextInterval = sortedIntervals[i]

            // Checks if there is an overlap.
            if (currentInterval.second >= nextInterval.first) {
                // Merges the intervals.
                currentInterval = Pair(
                    currentInterval.first,
                    maxOf(currentInterval.second, nextInterval.second)
                )
            } else {
                // Adds the current interval to the list of merged intervals.
                mergedIntervals.add(currentInterval)
                // Updates the current interval with the next one.
                currentInterval = nextInterval
            }
        }

        // Adds the last merged interval.
        mergedIntervals.add(currentInterval)

        return mergedIntervals
    }

    /**
     * @return MutableMap<Long, Array<Long>>.
     * Key: Long (Timestamp truncate to day).
     * Value: Array<Long> (Duration in milliseconds for each hour of the day).
     */
    private fun generateDurationMapByDayAndHour(
        intervals: List<Pair<Long, Long>>,
        startTimestamp: Long,
        endTimestamp: Long
    ): Map<Long, LongArray> {

        val durationsByDayAndHour = initializeDurationsByDayAndHour(startTimestamp, endTimestamp)

        for ((intervalStart, intervalEnd) in intervals) {

            var currentTimestamp: Long = intervalStart

            while (currentTimestamp < intervalEnd) {
                val dayTimestamp: Long = DateTimeUtils.truncateToDay(currentTimestamp)
                val hourIndex: Int = DateTimeUtils.getHourOfDay(currentTimestamp)

                // Computes the next split point: either the next full hour or intervalEnd
                val timestampTruncatedToNextHour: Long = DateTimeUtils.truncateToNextHour(currentTimestamp)
                val nextTimestamp = minOf(timestampTruncatedToNextHour, intervalEnd)

                // Add the elapsed time between the two intervals.
                val durationToAdd = nextTimestamp - currentTimestamp
                durationsByDayAndHour.getOrPut(dayTimestamp) { LongArray(24) }[hourIndex] += durationToAdd

                // Move to the next duration
                currentTimestamp = nextTimestamp
            }
        }

        return durationsByDayAndHour
    }

    private fun initializeDurationsByDayAndHour(
        startTimestamp: Long,
        endTimestamp: Long
    ): MutableMap<Long, LongArray> {
        val durationsByDayAndHour = mutableMapOf<Long, LongArray>()

        var currentTimestamp = DateTimeUtils.truncateToDay(startTimestamp)
        while(currentTimestamp < endTimestamp) {
            durationsByDayAndHour[currentTimestamp] = LongArray(24)
            currentTimestamp = DateTimeUtils.addDaysToTimestamp(currentTimestamp, 1)
        }

        return durationsByDayAndHour
    }

    suspend fun getMonthlyUsage(
        startTimestamp: Long,
        endTimestamp: Long,
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ): Pair<Map<String, Map<Long, Long>>, Map<Long, Long>> {
        val dayTimestamps = mutableListOf<Long>()
        var cur = DateTimeUtils.truncateToDay(startTimestamp)
        while (cur <= endTimestamp) {
            dayTimestamps.add(cur)
            cur = DateTimeUtils.addDaysToTimestamp(cur, 1)
        }

        val durationPerPackageByDay = mutableMapOf<String, MutableMap<Long, Long>>()
        val totalDurationByDay = dayTimestamps.associateWith { 0L }.toMutableMap()

        val statsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTimestamp,
            endTimestamp
        ) ?: emptyList()

        for (stat in statsList) {
            val pkg = stat.packageName ?: continue
            if (ignoreSystemApps && pkg !in launchableApp) continue
            if (ignoredPackages.contains(pkg)) continue
            if (stat.totalTimeInForeground <= 0) continue

            val day = dayTimestamps.minByOrNull { abs(it - stat.firstTimeStamp) }
            if (day != null && abs(day - stat.firstTimeStamp) <= 24L * 60 * 60 * 1000L) {
                val appMap = durationPerPackageByDay.getOrPut(pkg) {
                    dayTimestamps.associateWith { 0L }.toMutableMap()
                }
                appMap[day] = (appMap[day] ?: 0L) + stat.totalTimeInForeground
                totalDurationByDay[day] = (totalDurationByDay[day] ?: 0L) + stat.totalTimeInForeground
            }
        }

        return Pair(durationPerPackageByDay, totalDurationByDay)
    }

    suspend fun getYearlyUsage(
        startTimestamp: Long,
        endTimestamp: Long,
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ): Pair<Map<String, Map<Long, Long>>, Map<Long, Long>> {
        val monthTimestamps = mutableListOf<Long>()
        val cal = Calendar.getInstance()
        cal.timeInMillis = startTimestamp
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        for (m in 0..11) {
            cal.set(Calendar.MONTH, m)
            monthTimestamps.add(cal.timeInMillis)
        }

        val durationPerPackageByMonth = mutableMapOf<String, MutableMap<Long, Long>>()
        val totalDurationByMonth = monthTimestamps.associateWith { 0L }.toMutableMap()

        val statsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_MONTHLY,
            startTimestamp,
            endTimestamp
        ) ?: emptyList()

        for (stat in statsList) {
            val pkg = stat.packageName ?: continue
            if (ignoreSystemApps && pkg !in launchableApp) continue
            if (ignoredPackages.contains(pkg)) continue
            if (stat.totalTimeInForeground <= 0) continue

            val month = monthTimestamps.minByOrNull { abs(it - stat.firstTimeStamp) }
            if (month != null && abs(month - stat.firstTimeStamp) <= 32L * 24 * 60 * 60 * 1000L) {
                val appMap = durationPerPackageByMonth.getOrPut(pkg) {
                    monthTimestamps.associateWith { 0L }.toMutableMap()
                }
                appMap[month] = (appMap[month] ?: 0L) + stat.totalTimeInForeground
                totalDurationByMonth[month] = (totalDurationByMonth[month] ?: 0L) + stat.totalTimeInForeground
            }
        }

        return Pair(durationPerPackageByMonth, totalDurationByMonth)
    }

    suspend fun getAllUsage(
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ): Pair<Map<String, Map<Long, Long>>, Map<Long, Long>> {
        val now = System.currentTimeMillis()
        val statsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_YEARLY,
            0L,
            now
        ) ?: emptyList()

        val yearSet = sortedSetOf<Long>()
        val currentYear = DateTimeUtils.truncateToYear(now)
        yearSet.add(currentYear)

        for (stat in statsList) {
            if (stat.firstTimeStamp > 0) {
                yearSet.add(DateTimeUtils.truncateToYear(stat.firstTimeStamp))
            }
        }

        val yearTimestamps = yearSet.toList()
        val durationPerPackageByYear = mutableMapOf<String, MutableMap<Long, Long>>()
        val totalDurationByYear = yearTimestamps.associateWith { 0L }.toMutableMap()

        for (stat in statsList) {
            val pkg = stat.packageName ?: continue
            if (ignoreSystemApps && pkg !in launchableApp) continue
            if (ignoredPackages.contains(pkg)) continue
            if (stat.totalTimeInForeground <= 0) continue

            val year = yearTimestamps.minByOrNull { abs(it - stat.firstTimeStamp) }
            if (year != null) {
                val appMap = durationPerPackageByYear.getOrPut(pkg) {
                    yearTimestamps.associateWith { 0L }.toMutableMap()
                }
                appMap[year] = (appMap[year] ?: 0L) + stat.totalTimeInForeground
                totalDurationByYear[year] = (totalDurationByYear[year] ?: 0L) + stat.totalTimeInForeground
            }
        }

        if (totalDurationByYear.values.sum() == 0L) {
            val aggregated = usageStatsManager.queryAndAggregateUsageStats(0L, now)
            for ((pkg, stat) in aggregated) {
                if (ignoreSystemApps && pkg !in launchableApp) continue
                if (ignoredPackages.contains(pkg)) continue
                if (stat.totalTimeInForeground <= 0) continue

                val appMap = durationPerPackageByYear.getOrPut(pkg) {
                    yearTimestamps.associateWith { 0L }.toMutableMap()
                }
                appMap[currentYear] = (appMap[currentYear] ?: 0L) + stat.totalTimeInForeground
                totalDurationByYear[currentYear] = (totalDurationByYear[currentYear] ?: 0L) + stat.totalTimeInForeground
            }
        }

        return Pair(durationPerPackageByYear, totalDurationByYear)
    }
}