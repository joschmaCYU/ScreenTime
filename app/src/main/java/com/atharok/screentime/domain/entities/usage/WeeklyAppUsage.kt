package com.atharok.screentime.domain.entities.usage

import android.graphics.drawable.Drawable
import com.atharok.screentime.common.extensions.safeDivideBy
import com.atharok.screentime.common.utils.DateTimeUtils
import kotlin.math.roundToLong

class WeeklyAppUsage(
    packageName: String,
    appName: String,
    appIcon: Drawable?,
    val durations: Map<Long, Array<Long>>
) : AppUsage(packageName, appName, appIcon) {

    private val _totalTimeUsed: Long = durations.values.sumOf { array -> array.sum() }

    private val _averageTimeUsed: Long = _totalTimeUsed.safeDivideBy(durations.size).roundToLong()

    private val _chartValues: Map<String, Float> by lazy {
        durations.map { (dayTimestamp: Long, hourTimestamps: Array<Long>) ->
            DateTimeUtils.formatToDayOfTheWeek(dayTimestamp) to DateTimeUtils.convertToHourFloat(hourTimestamps.sum())
        }.toMap()
    }
    
    override fun getTotalTimeUsed(): Long = _totalTimeUsed

    override fun getAverageTimeUsed(): Long = _averageTimeUsed

    override fun getChartValues(): Map<String, Float> = _chartValues
}