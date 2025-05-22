package com.atharok.screentime.ui.views

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atharok.screentime.common.utils.DateTimeUtils
import com.atharok.screentime.domain.entities.Period
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.component.TextComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import org.koin.compose.koinInject
import kotlin.math.floor

@Composable
fun Chart(
    period: Period,
    data: Map<String, Float>,
    modifier: Modifier = Modifier,
    modelProducer: CartesianChartModelProducer = koinInject()
) {

    val bottomAxisLabelListKey = remember { ExtraStore.Key<List<String>>() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries {
                series(data.values)
            }
            extras {
                it[bottomAxisLabelListKey] = data.keys.toList()
            }
        }
    }

    StatelessChart(
        data = data,
        bottomAxis = when(period) {
            Period.WEEK -> {
                rememberBottomAxisDefault(
                    valueFormatter = remember {{ cartesianMeasuringContext, x, _ ->
                        val xInt: Int = x.toInt()
                        cartesianMeasuringContext.model.extraStore.getOrNull(bottomAxisLabelListKey)?.getOrNull(xInt) ?: "$xInt"
                    }},
                    itemPlacer = HorizontalAxis.ItemPlacer.aligned(addExtremeLabelPadding = true)
                )
            }
            Period.DAY -> {
                rememberBottomAxisDefault(
                    valueFormatter = remember{{ cartesianMeasuringContext, x, _  ->
                        val xInt: Int = x.toInt()
                        if(xInt % 4 == 0) cartesianMeasuringContext.model.extraStore.getOrNull(bottomAxisLabelListKey)?.getOrNull(xInt) ?: "$xInt" else "$xInt"
                    }},
                    itemPlacer = HorizontalAxis.ItemPlacer.aligned(spacing = { 4 }, addExtremeLabelPadding = true)
                )
            }
        },
        modifier = modifier,
        modelProducer = modelProducer
    )
}

@Composable
private fun StatelessChart(
    data: Map<String, Float>,
    bottomAxis: Axis<Axis.Position.Horizontal.Bottom>,
    modifier: Modifier,
    modelProducer: CartesianChartModelProducer
) {
    ProvideVicoTheme(theme = rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                        vicoTheme.columnCartesianLayerColors.map { color ->
                            rememberLineComponent(
                                fill = fill(color),
                                thickness = 4.dp,
                                shape = CorneredShape.Pill
                            )
                        }
                    )
                ),
                startAxis = VerticalAxis.rememberStart(
                    line = rememberAxisLineComponentDefault(),
                    label = rememberAxisLabelComponentDefault(),
                    tick = rememberAxisTickGuidelineComponentDefault(),
                    guideline = rememberAxisGuidelineComponentDefault(),
                    valueFormatter = remember {
                        { _, y: Double, _ ->
                            DateTimeUtils.formatToHoursMinutes(y.toFloat())
                        }
                    },
                    itemPlacer = VerticalAxis.ItemPlacer.step(
                        step = {
                            val max = data.values.max()
                            (if(max >= 1f) 0.25f + floor(max) * 0.25f else 1f / 60f).toDouble()
                        }
                    )
                ),
                bottomAxis = bottomAxis,

                // Average
                decorations = listOf(
                    HorizontalLine(
                        y = {
                            (data.values.sum() / data.values.size).toDouble()
                        },
                        line = LineComponent(
                            fill = fill(MaterialTheme.colorScheme.tertiary),
                            shape = CorneredShape.Pill
                        )
                    )
                )
            ),
            modelProducer = modelProducer,
            modifier = modifier,
            scrollState = rememberVicoScrollState(false),
        )
    }
}

// ---- Axis ----

@Composable
private fun rememberBottomAxisDefault(
    valueFormatter: CartesianValueFormatter = remember { CartesianValueFormatter.decimal() },
    itemPlacer: HorizontalAxis.ItemPlacer = remember { HorizontalAxis.ItemPlacer.aligned() },
): HorizontalAxis<Axis.Position.Horizontal.Bottom> = HorizontalAxis.rememberBottom(
    line = rememberAxisLineComponentDefault(),
    label = rememberAxisLabelComponentDefault(),
    tick = rememberAxisTickGuidelineComponentDefault(),
    guideline = rememberAxisGuidelineComponentDefault(),
    valueFormatter = valueFormatter,
    itemPlacer = itemPlacer
)

@Composable
private fun rememberAxisLabelComponentDefault(): TextComponent = rememberAxisLabelComponent(
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textSize = 12.sp,
    typeface = Typeface.DEFAULT
)

@Composable
private fun rememberAxisLineComponentDefault(): LineComponent = rememberAxisLineComponent(
    fill = fill(MaterialTheme.colorScheme.outline)
)

@Composable
private fun rememberAxisTickGuidelineComponentDefault(): LineComponent = rememberAxisGuidelineComponent(
    fill = fill(MaterialTheme.colorScheme.outline)
)

@Composable
private fun rememberAxisGuidelineComponentDefault(): LineComponent = rememberAxisGuidelineComponent(
    fill = fill(MaterialTheme.colorScheme.outlineVariant)
)