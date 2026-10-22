package com.atharok.screentime.domain.entities.usage

import com.atharok.screentime.common.extensions.median
import com.atharok.screentime.common.extensions.safeDivideBy
import com.atharok.screentime.common.utils.DateTimeUtils
import kotlin.math.roundToLong

class AllDeviceUsage(
    usages: Map<String, AllAppUsage>,
    timeInterval: Pair<Long, Long>,
    dateOfLastRefresh: String,
    val totalDeviceUsageByYear: Map<Long, Long>
) : DeviceUsage<AllAppUsage>(usages, timeInterval, dateOfLastRefresh) {

    private val _totalTimeUsed: Long = totalDeviceUsageByYear.values.sum()

    private val _averageTimeUsed: Long =
        _totalTimeUsed.safeDivideBy(totalDeviceUsageByYear.size.coerceAtLeast(1)).roundToLong()

    private val _medianTimeUsed: Long by lazy { totalDeviceUsageByYear.values.toLongArray().median() }

    private val _chartValues: Map<String, Float> = totalDeviceUsageByYear
        .map { (yearTimestamp: Long, duration: Long) ->
            DateTimeUtils.formatToYear(yearTimestamp) to DateTimeUtils.convertToHourFloat(duration)
        }.toMap()

    override fun getTotalTimeUsed(): Long = _totalTimeUsed

    override fun getAverageTimeUsed(): Long = _averageTimeUsed

    override fun getMedianTimeUsed(): Long = _medianTimeUsed

    override fun getChartValues(): Map<String, Float> = _chartValues
}
