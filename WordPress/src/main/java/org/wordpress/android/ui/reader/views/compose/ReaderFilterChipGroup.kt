package org.wordpress.android.ui.reader.views.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    filterChips: List<ReaderFilterChipType>,
) {
    AnimatedContent(
        modifier = modifier,
        label = "ReaderFilterChipGroup",
        targetState = filterChips,
        transitionSpec = {
            val selectedItem = targetState.filterIsInstance<ReaderFilterChipType.SelectedItem>().firstOrNull()
            if (selectedItem != null) {
                // item is being selected
                ContentTransform(
                    slideInVertically(tween()) { height -> height } + fadeIn(tween()),
                    slideOutVertically(tween()) { height -> -height } + fadeOut(tween()),
                )
            } else {
                // going back to the list of filters
                ContentTransform(
                    slideInVertically(tween()) { height -> -height } + fadeIn(tween()),
                    slideOutVertically(tween()) { height -> height } + fadeOut(tween()),
                )
            }.using(
                SizeTransform(
                    clip = false,
                    sizeAnimationSpec = { initialSize, targetSize ->
                        if (targetSize.width > initialSize.width) {
                            snap()
                        } else {
                            tween(delayMillis = AnimationConstants.DefaultDurationMillis)
                        }
                    }
                )
            )
        }
    ) { targetFilterChips ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            targetFilterChips.forEach { item ->
                when (item) {
                    is ReaderFilterChipType.Filter -> {
                        ReaderFilterChip(
                            text = item.text,
                            onClick = item.onClick,
                        )
                    }

                    is ReaderFilterChipType.SelectedItem -> {
                        ReaderFilterChip(
                            text = item.text,
                            onClick = item.onClick,
                            onDismissClick = item.onDismissClick,
                            invertColors = true,
                        )
                    }
                }
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
                    modifier = Modifier.height(20.dp),
                )

                if (onDismissClick != null) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null, // TODO thomashorta clear or dismiss?
                        modifier = Modifier
                            .size(20.dp)
                            .padding(2.dp)
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
    data class Filter(
        override val text: UiString,
        override val onClick: () -> Unit,
    ) : ReaderFilterChipType(text, onClick)

    data class SelectedItem(
        override val text: UiString,
        override val onClick: () -> Unit,
        val onDismissClick: () -> Unit,
    ) : ReaderFilterChipType(text, onClick)
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ReaderFilterChipGroupPreview() {
    lateinit var allChips: MutableState<List<ReaderFilterChipType>>
    lateinit var clearSelection: () -> Unit
    val createSelectedItem: (String) -> ReaderFilterChipType.SelectedItem = {
        ReaderFilterChipType.SelectedItem(
            text = UiString.UiStringText(it),
            onClick = clearSelection,
            onDismissClick = clearSelection,
        )
    }
    val filterChips = listOf(
        ReaderFilterChipType.Filter(
            text = UiString.UiStringText("23 Blogs"),
            onClick = {
                allChips.value = listOf(createSelectedItem("Amazing Blog"))
            },
        ),
        ReaderFilterChipType.Filter(
            text = UiString.UiStringText("41 Tags"),
            onClick = {
                allChips.value = listOf(createSelectedItem("Amazing Tag"))
            },
        ),
    )

    clearSelection = { allChips.value = filterChips }

    allChips = remember { mutableStateOf(filterChips) }

    AppThemeWithoutBackground {
        ReaderFilterChipGroup(
            modifier = Modifier
                .padding(8.dp),
            filterChips = allChips.value,
        )
    }
}
