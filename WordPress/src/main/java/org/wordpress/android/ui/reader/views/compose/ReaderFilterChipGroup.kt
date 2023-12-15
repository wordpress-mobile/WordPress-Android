package org.wordpress.android.ui.reader.views.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.theme.AppThemeWithoutBackground
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.utils.UiString
import androidx.compose.material3.MaterialTheme as Material3Theme

@Composable
fun ReaderFilterChipGroup(
    modifier: Modifier = Modifier,
    filterCategories: List<ReaderFilterChipType.FilterCategory>,
    selectedFilterChoice: ReaderFilterChipType.SelectedFilterChoice? = null,
) {
    var currentSelectedFilterChoice by remember { mutableStateOf(selectedFilterChoice) }
    val showSelectedFilter = selectedFilterChoice != null

    if (selectedFilterChoice != null) {
        currentSelectedFilterChoice = selectedFilterChoice
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filterCategories.forEach { filterCategory ->
            AnimatedVisibility(visible = !showSelectedFilter) {
                ReaderFilterChip(
                    text = filterCategory.text,
                    onClick = filterCategory.onClick,
                )
            }
        }

        AnimatedVisibility(visible = showSelectedFilter) {
            currentSelectedFilterChoice?.let { selectedFilterChoice ->
                ReaderFilterChip(
                    text = selectedFilterChoice.text,
                    onClick = selectedFilterChoice.onClick,
                    onDismissClick = selectedFilterChoice.onDismissClick,
                    invertColors = true,
                )
            }
        }
    }
}

@Composable
fun ReaderFilterChip(
    text: UiString,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    invertColors: Boolean = false,
    onDismissClick: (() -> Unit)? = null,
) {
    val padding = PaddingValues(
        top = 8.dp,
        bottom = 8.dp,
        start = 24.dp,
        end = if (onDismissClick != null) 12.dp else 24.dp,
    )

    CompositionLocalProvider(
        LocalContentColor provides if (invertColors) {
            MaterialTheme.colors.surface
        } else {
            MaterialTheme.colors.onSurface
        },
    ) {
        Box(
            modifier = modifier
                .background(
                    color = if (invertColors) {
                        MaterialTheme.colors.onSurface
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(50),
                )
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier.padding(padding),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    uiStringText(text),
                    style = Material3Theme.typography.titleSmall,
                )

                if (onDismissClick != null) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null, // TODO thomashorta clear or dismiss?
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp)
                            .clickable(onClick = onDismissClick),
                    )
                }
            }
        }
    }
}

sealed class ReaderFilterChipType(
    open val text: UiString,
    open val onClick: () -> Unit,
) {
    data class FilterCategory(
        override val text: UiString,
        override val onClick: () -> Unit,
    ) : ReaderFilterChipType(text, onClick)

    data class SelectedFilterChoice(
        override val text: UiString,
        override val onClick: () -> Unit,
        val onDismissClick: () -> Unit,
    ) : ReaderFilterChipType(text, onClick)
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ReaderFilterChipGroupPreview() {
    var selectedTag: ReaderFilterChipType.SelectedFilterChoice? by remember { mutableStateOf(null) }
    val clearSelection = { selectedTag = null }

    AppThemeWithoutBackground {
        ReaderFilterChipGroup(
            modifier = Modifier.padding(8.dp),
            filterCategories = listOf(
                ReaderFilterChipType.FilterCategory(
                    text = UiString.UiStringText("23 Blogs"),
                    onClick = {
                        selectedTag = ReaderFilterChipType.SelectedFilterChoice(
                            text = UiString.UiStringText("Amazing Blog"),
                            onClick = clearSelection,
                            onDismissClick = clearSelection,
                        )
                    },
                ),
                ReaderFilterChipType.FilterCategory(
                    text = UiString.UiStringText("41 Tags"),
                    onClick = {
                        selectedTag = ReaderFilterChipType.SelectedFilterChoice(
                            text = UiString.UiStringText("Amazing Tag"),
                            onClick = clearSelection,
                            onDismissClick = clearSelection,
                        )
                    },
                ),
            ),
            selectedFilterChoice = selectedTag,
        )
    }
}
