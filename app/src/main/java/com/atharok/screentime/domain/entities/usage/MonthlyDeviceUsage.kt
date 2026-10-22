package com.atharok.screentime.domain.entities.usage

import com.atharok.screentime.common.extensions.median
import com.atharok.screentime.common.extensions.safeDivideBy
import com.atharok.screentime.common.utils.DateTimeUtils
import kotlin.math.roundToLong

class MonthlyDeviceUsage(
    usages: Map<String, MonthlyAppUsage>,
    timeInterval: Pair<Long, Long>,
    dateOfLastRefresh: String,
    val totalDeviceUsageByDay: Map<Long, Long>
) : DeviceUsage<MonthlyAppUsage>(usages, timeInterval, dateOfLastRefresh) {

    private val _totalTimeUsed: Long = totalDeviceUsageByDay.values.sum()

    private val _averageTimeUsed: Long =
        _totalTimeUsed.safeDivideBy(totalDeviceUsageByDay.size.coerceAtLeast(1)).roundToLong()

    private val _medianTimeUsed: Long by lazy { totalDeviceUsageByDay.values.toLongArray().median() }

    private val _chartValues: Map<String, Float> = totalDeviceUsageByDay
        .map { (dayTimestamp: Long, duration: Long) ->
            DateTimeUtils.formatToDayOfMonth(dayTimestamp) to DateTimeUtils.convertToHourFloat(duration)
        }.toMap()

    override fun getTotalTimeUsed(): Long = _totalTimeUsed

    override fun getAverageTimeUsed(): Long = _averageTimeUsed

    override fun getMedianTimeUsed(): Long = _medianTimeUsed

    override fun getChartValues(): Map<String, Float> = _chartValues
}
