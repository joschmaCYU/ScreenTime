package com.atharok.screentime.presentation.glance

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class TodayUsageWidgetDimensions {

    val height: Dp
    val padding: Dp
    val minIconSize: Dp
    val maxIconSize: Dp
    val minTextSp: TextUnit
    val standardTextSp: TextUnit
    val screenTimeTextSp: TextUnit

    private constructor(
        height: Dp,
        padding: Dp,
        minIconSize: Dp,
        maxIconSize: Dp,
        minTextSp: TextUnit,
        standardTextSp: TextUnit,
        screenTimeTextSp: TextUnit
    ) {
        this.height = height
        this.padding = padding
        this.minTextSp = minTextSp
        this.standardTextSp = standardTextSp
        this.minIconSize = minIconSize
        this.maxIconSize = maxIconSize
        this.screenTimeTextSp = screenTimeTextSp
    }

    companion object {
        fun calculateDimensions(height: Dp): TodayUsageWidgetDimensions {
            return when {
                height > 224.dp -> {
                    TodayUsageWidgetDimensions(
                        height = height,
                        padding = 12.dp,
                        minIconSize = 36.dp,
                        maxIconSize = 48.dp,
                        minTextSp = 12.sp,
                        standardTextSp = 14.sp,
                        screenTimeTextSp = 36.sp
                    )
                }

                height > 192.dp -> {
                    TodayUsageWidgetDimensions(
                        height = height,
                        padding = 10.dp,
                        minIconSize = 32.dp,
                        maxIconSize = 44.dp,
                        minTextSp = 11.sp,
                        standardTextSp = 13.sp,
                        screenTimeTextSp = 32.sp
                    )
                }

                height > 112.dp -> {
                    TodayUsageWidgetDimensions(
                        height = height,
                        padding = 12.dp,
                        minIconSize = 36.dp,
                        maxIconSize = 48.dp,
                        minTextSp = 12.sp,
                        standardTextSp = 14.sp,
                        screenTimeTextSp = 36.sp
                    )
                }

                height > 88.dp -> {
                    TodayUsageWidgetDimensions(
                        height = height,
                        padding = 10.dp,
                        minIconSize = 32.dp,
                        maxIconSize = 44.dp,
                        minTextSp = 11.sp,
                        standardTextSp = 13.sp,
                        screenTimeTextSp = 32.sp
                    )
                }

                else -> {
                    TodayUsageWidgetDimensions(
                        height = height,
                        padding = 8.dp,
                        minIconSize = 28.dp,
                        maxIconSize = 40.dp,
                        minTextSp = 10.sp,
                        standardTextSp = 12.sp,
                        screenTimeTextSp = 28.sp
                    )
                }
            }
        }
    }
}