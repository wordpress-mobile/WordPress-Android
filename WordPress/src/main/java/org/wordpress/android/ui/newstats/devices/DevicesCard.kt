package org.wordpress.android.ui.newstats.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.StatsCardContainer
import org.wordpress.android.ui.newstats.components.StatsCardEmptyContent
import org.wordpress.android.ui.newstats.components.StatsCardErrorContent
import org.wordpress.android.ui.newstats.components.StatsCardHeader
import org.wordpress.android.ui.newstats.components.StatsListHeader
import org.wordpress.android.ui.newstats.components.StatsListRowContainer
import org.wordpress.android.ui.newstats.components.StatsItemName
import org.wordpress.android.ui.newstats.util.ShimmerBox
import java.util.Locale

private val CardPadding = 16.dp
private const val LOADING_ITEM_COUNT = 4
private const val CHART_SIZE_FRACTION = 0.5f

@Composable
fun DevicesCard(
    uiState: DevicesCardUiState,
    selectedDeviceType: DeviceType,
    onDeviceTypeChanged: (DeviceType) -> Unit,
    onRetry: () -> Unit,
    onRemoveCard: () -> Unit,
    modifier: Modifier = Modifier,
    cardPosition: CardPosition? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null,
    onOpenWpAdmin: (() -> Unit)? = null
) {
    StatsCardContainer(modifier = modifier) {
        when (uiState) {
            is DevicesCardUiState.Loading -> LoadingContent(
                selectedDeviceType = selectedDeviceType,
                onDeviceTypeChanged = onDeviceTypeChanged,
                onRemoveCard = onRemoveCard,
                cardPosition = cardPosition,
                onMoveUp = onMoveUp,
                onMoveToTop = onMoveToTop,
                onMoveDown = onMoveDown,
                onMoveToBottom = onMoveToBottom
            )
            is DevicesCardUiState.Loaded -> LoadedContent(
                uiState, selectedDeviceType,
                onDeviceTypeChanged, onRemoveCard,
                cardPosition, onMoveUp, onMoveToTop,
                onMoveDown, onMoveToBottom
            )
            is DevicesCardUiState.Error -> {
                StatsCardErrorContent(
                    titleResId = R.string.stats_devices_title,
                    errorMessageResId = uiState.messageResId,
                    onRetry = onRetry,
                    onRemoveCard = onRemoveCard,
                    cardPosition = cardPosition,
                    onMoveUp = onMoveUp,
                    onMoveToTop = onMoveToTop,
                    onMoveDown = onMoveDown,
                    onMoveToBottom = onMoveToBottom,
                    onOpenWpAdmin = onOpenWpAdmin,
                    headerExtra = {
                        DeviceTypeSelector(
                            selectedType = selectedDeviceType,
                            onTypeSelected = onDeviceTypeChanged
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(
    selectedDeviceType: DeviceType,
    onDeviceTypeChanged: (DeviceType) -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    Column(modifier = Modifier.padding(CardPadding)) {
        StatsCardHeader(
            titleResId = R.string.stats_devices_title,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(8.dp))

        DeviceTypeSelector(
            selectedType = selectedDeviceType,
            onTypeSelected = onDeviceTypeChanged
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Ring chart placeholder
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ShimmerBox(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        StatsListHeader(
            leftHeaderResId =
                R.string.stats_devices_device_header
        )
        Spacer(modifier = Modifier.height(8.dp))

        repeat(LOADING_ITEM_COUNT) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 12.dp,
                        horizontal = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                ShimmerBox(
                    modifier = Modifier
                        .width(50.dp)
                        .height(16.dp)
                )
            }
            if (index < LOADING_ITEM_COUNT - 1) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun LoadedContent(
    state: DevicesCardUiState.Loaded,
    selectedDeviceType: DeviceType,
    onDeviceTypeChanged: (DeviceType) -> Unit,
    onRemoveCard: () -> Unit,
    cardPosition: CardPosition?,
    onMoveUp: (() -> Unit)?,
    onMoveToTop: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveToBottom: (() -> Unit)?
) {
    Column(modifier = Modifier.padding(CardPadding)) {
        StatsCardHeader(
            titleResId = R.string.stats_devices_title,
            onRemoveCard = onRemoveCard,
            cardPosition = cardPosition,
            onMoveUp = onMoveUp,
            onMoveToTop = onMoveToTop,
            onMoveDown = onMoveDown,
            onMoveToBottom = onMoveToBottom
        )
        Spacer(modifier = Modifier.height(8.dp))

        DeviceTypeSelector(
            selectedType = selectedDeviceType,
            onTypeSelected = onDeviceTypeChanged
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (state.items.isEmpty()) {
            StatsCardEmptyContent()
        } else {
            val colors = ringChartColors()
            val chartEntries = state.items.mapIndexed { index, item ->
                RingChartEntry(
                    label = item.name,
                    value = item.value,
                    color = colors[index % colors.size]
                )
            }

            // Ring chart
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                StatsRingChart(
                    entries = chartEntries,
                    modifier = Modifier
                        .fillMaxWidth(CHART_SIZE_FRACTION)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            val isPercentage =
                selectedDeviceType == DeviceType.SCREENSIZE
            StatsListHeader(
                leftHeaderResId =
                    R.string.stats_devices_device_header,
                rightHeaderResId = if (isPercentage) {
                    R.string.stats_devices_percentage_header
                } else {
                    R.string.stats_countries_views_header
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            state.items.forEachIndexed { index, item ->
                val barFraction =
                    if (state.maxValueForBar > 0.0) {
                        (item.value / state.maxValueForBar)
                            .toFloat()
                    } else {
                        0f
                    }
                DeviceRow(
                    item = item,
                    percentage = barFraction,
                    color = colors[index % colors.size],
                    isPercentage = isPercentage
                )
                if (index < state.items.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    item: DeviceItem,
    percentage: Float,
    color: Color,
    isPercentage: Boolean
) {
    StatsListRowContainer(percentage = percentage) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            StatsItemName(
                name = item.name,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatDeviceValue(
                    item.value, isPercentage
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceTypeSelector(
    selectedType: DeviceType,
    onTypeSelected: (DeviceType) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        DeviceType.entries.forEachIndexed { index, type ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = DeviceType.entries.size
                ),
                onClick = { onTypeSelected(type) },
                selected = type == selectedType,
                icon = {}
            ) {
                Text(text = stringResource(type.labelResId))
            }
        }
    }
}

@Suppress("ReturnCount")
private fun formatDeviceValue(
    value: Double,
    isPercentage: Boolean
): String = when {
    isPercentage -> {
        // Round to nearest int; show "-" if the result is 0
        // (covers both exact 0.0 and near-zero like 0.3)
        val rounded = Math.round(value)
        if (rounded == 0L) "-" else "$rounded%"
    }
    value == 0.0 -> "-"
    value % 1.0 == 0.0 -> value.toLong().toString()
    else -> String.format(
        Locale.getDefault(), "%.1f", value
    )
}

// Previews
@Preview(showBackground = true)
@Composable
private fun DevicesCardLoadingPreview() {
    AppThemeM3 {
        DevicesCard(
            uiState = DevicesCardUiState.Loading,
            selectedDeviceType = DeviceType.SCREENSIZE,
            onDeviceTypeChanged = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DevicesCardLoadedPreview() {
    AppThemeM3 {
        DevicesCard(
            uiState = DevicesCardUiState.Loaded(
                items = listOf(
                    DeviceItem("Desktop", 57.6),
                    DeviceItem("Mobile", 23.9),
                    DeviceItem("Tablet", 0.5)
                ),
                maxValueForBar = 57.6
            ),
            selectedDeviceType = DeviceType.SCREENSIZE,
            onDeviceTypeChanged = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DevicesCardErrorPreview() {
    AppThemeM3 {
        DevicesCard(
            uiState = DevicesCardUiState.Error(
                R.string.stats_error_api
            ),
            selectedDeviceType = DeviceType.SCREENSIZE,
            onDeviceTypeChanged = {},
            onRetry = {},
            onRemoveCard = {}
        )
    }
}
