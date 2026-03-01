package com.atharok.screentime.presentation.glance

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class TodayUsageWidgetDimensions {

    val width: Dp
    val height: Dp
    val padding: Dp
    val iconSize: Dp
    val minTextSp: TextUnit
    val standardTextSp: TextUnit
    val screenTimeTextSp: TextUnit

    private constructor(
        width: Dp,
        height: Dp,
        padding: Dp,
        iconSize: Dp,
        minTextSp: TextUnit,
        standardTextSp: TextUnit,
        screenTimeTextSp: TextUnit
    ) {
        this.width = width
        this.height = height
        this.padding = padding
        this.iconSize = iconSize
        this.minTextSp = minTextSp
        this.standardTextSp = standardTextSp
        this.screenTimeTextSp = screenTimeTextSp
    }

    companion object {

        val WIDTH_MIN = 192.dp

        fun calculateDimensions(width: Dp, height: Dp): TodayUsageWidgetDimensions {
            return when {
                height > 224.dp -> {
                    TodayUsageWidgetDimensions(
                        width = width,
                        height = height,
                        padding = 12.dp,
                        iconSize = if(width < WIDTH_MIN) 28.dp else 36.dp,
                        minTextSp = 12.sp,
                        standardTextSp = 14.sp,
                        screenTimeTextSp = if(width < WIDTH_MIN) 32.sp else 36.sp
                    )
                }

                height > 192.dp -> {
                    TodayUsageWidgetDimensions(
                        width = width,
                        height = height,
                        padding = 10.dp,
                        iconSize = if(width < WIDTH_MIN) 24.dp else 32.dp,
                        minTextSp = 11.sp,
                        standardTextSp = 13.sp,
                        screenTimeTextSp = if(width < WIDTH_MIN) 28.sp else 32.sp
                    )
                }

                height > 112.dp -> {
                    TodayUsageWidgetDimensions(
                        width = width,
                        height = height,
                        padding = 12.dp,
                        iconSize = if(width < WIDTH_MIN) 28.dp else 36.dp,
                        minTextSp = 12.sp,
                        standardTextSp = 14.sp,
                        screenTimeTextSp = if(width < WIDTH_MIN) 32.sp else 36.sp
                    )
                }

                height > 88.dp -> {
                    TodayUsageWidgetDimensions(
                        width = width,
                        height = height,
                        padding = 10.dp,
                        iconSize = if(width < WIDTH_MIN) 24.dp else 32.dp,
                        minTextSp = 11.sp,
                        standardTextSp = 13.sp,
                        screenTimeTextSp = if(width < WIDTH_MIN) 28.sp else 32.sp
                    )
                }

                else -> {
                    TodayUsageWidgetDimensions(
                        width = width,
                        height = height,
                        padding = 8.dp,
                        iconSize = if(width < WIDTH_MIN) 20.dp else 28.dp,
                        minTextSp = 10.sp,
                        standardTextSp = 12.sp,
                        screenTimeTextSp = if(width < WIDTH_MIN) 24.sp else 28.sp
                    )
                }
            }
        }
    }
}