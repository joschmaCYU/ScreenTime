package com.atharok.screentime.ui.views

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.atharok.screentime.domain.entities.Period
import com.atharok.screentime.ui.components.TextNormal

@Composable
fun PeriodSelector(
    period: Period,
    onPeriodChange: (Period) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods = Period.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        periods.forEachIndexed { index, p ->
            SegmentedButton(
                selected = period == p,
                onClick = { onPeriodChange(p) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size)
            ) {
                TextNormal(
                    text = stringResource(id = p.stringRes),
                    maxLines = 1
                )
            }
        }
    }
}
