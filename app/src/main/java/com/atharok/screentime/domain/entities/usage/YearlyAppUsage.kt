package com.atharok.screentime.domain.entities.usage

import android.graphics.drawable.Drawable
import com.atharok.screentime.common.extensions.median
import com.atharok.screentime.common.extensions.safeDivideBy
import com.atharok.screentime.common.utils.DateTimeUtils
import kotlin.math.roundToLong

class YearlyAppUsage(
    packageName: String,
    appName: String,
    appIcon: Drawable?,
    val durations: Map<Long, Long>
) : AppUsage(packageName, appName, appIcon) {

    private val _totalTimeUsed: Long = durations.values.sum()

    private val _averageTimeUsed: Long = _totalTimeUsed.safeDivideBy(durations.size.coerceAtLeast(1)).roundToLong()

    private val _medianTimeUsed: Long by lazy { durations.values.toLongArray().median() }

    private val _chartValues: Map<String, Float> by lazy {
        durations.map { (monthTimestamp: Long, duration: Long) ->
            DateTimeUtils.formatToMonthShort(monthTimestamp) to DateTimeUtils.convertToHourFloat(duration)
        }.toMap()
    }

    override fun getTotalTimeUsed(): Long = _totalTimeUsed

    override fun getAverageTimeUsed(): Long = _averageTimeUsed

    override fun getMedianTimeUsed(): Long = _medianTimeUsed

    override fun getChartValues(): Map<String, Float> = _chartValues
}
