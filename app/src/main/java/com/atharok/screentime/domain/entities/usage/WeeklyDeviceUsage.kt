package com.atharok.screentime.domain.entities.usage

import com.atharok.screentime.common.extensions.safeDivideBy
import com.atharok.screentime.common.utils.DateTimeUtils
import kotlin.math.roundToLong

class WeeklyDeviceUsage(
    usages: Map<String, WeeklyAppUsage>,
    timeInterval: Pair<Long, Long>,
    dateOfLastRefresh: String,
    totalDeviceUsageByDayAndHour: Map<Long, Array<Long>>
) : DeviceUsage<WeeklyAppUsage>(usages, timeInterval, dateOfLastRefresh) {

    private val _totalTimeUsed: Long = totalDeviceUsageByDayAndHour.values.sumOf { it.sum() }

    private val _averageTimeUsed: Long =
        _totalTimeUsed.safeDivideBy(totalDeviceUsageByDayAndHour.size).roundToLong()

    private val _chartValues: Map<String, Float> = totalDeviceUsageByDayAndHour
        .map { (dayTimestamp: Long, hourTimestamps: Array<Long>) ->
            DateTimeUtils.formatToDayOfTheWeek(dayTimestamp) to DateTimeUtils.convertToHourFloat(hourTimestamps.sum())
        }.toMap()

    override fun getTotalTimeUsed(): Long = _totalTimeUsed

    override fun getAverageTimeUsed(): Long = _averageTimeUsed

    override fun getChartValues(): Map<String, Float> = _chartValues
}