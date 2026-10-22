package com.atharok.screentime.domain.entities.usage

import com.atharok.screentime.common.extensions.median
import com.atharok.screentime.common.extensions.safeDivideBy
import com.atharok.screentime.common.utils.DateTimeUtils
import kotlin.math.roundToLong

class YearlyDeviceUsage(
    usages: Map<String, YearlyAppUsage>,
    timeInterval: Pair<Long, Long>,
    dateOfLastRefresh: String,
    val totalDeviceUsageByMonth: Map<Long, Long>
) : DeviceUsage<YearlyAppUsage>(usages, timeInterval, dateOfLastRefresh) {

    private val _totalTimeUsed: Long = totalDeviceUsageByMonth.values.sum()

    private val _averageTimeUsed: Long =
        _totalTimeUsed.safeDivideBy(totalDeviceUsageByMonth.size.coerceAtLeast(1)).roundToLong()

    private val _medianTimeUsed: Long by lazy { totalDeviceUsageByMonth.values.toLongArray().median() }

    private val _chartValues: Map<String, Float> = totalDeviceUsageByMonth
        .map { (monthTimestamp: Long, duration: Long) ->
            DateTimeUtils.formatToMonthShort(monthTimestamp) to DateTimeUtils.convertToHourFloat(duration)
        }.toMap()

    override fun getTotalTimeUsed(): Long = _totalTimeUsed

    override fun getAverageTimeUsed(): Long = _averageTimeUsed

    override fun getMedianTimeUsed(): Long = _medianTimeUsed

    override fun getChartValues(): Map<String, Float> = _chartValues
}
