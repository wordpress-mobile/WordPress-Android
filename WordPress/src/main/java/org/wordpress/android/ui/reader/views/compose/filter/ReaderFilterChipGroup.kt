package org.wordpress.android.ui.reader.views.compose.filter

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeWithoutBackground
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.utils.UiString
import androidx.compose.material3.MaterialTheme as Material3Theme

private val roundedShape = RoundedCornerShape(100)

@Composable
fun ReaderFilterChipGroup(
    followedBlogsCount: Int,
    followedTagsCount: Int,
    onFilterClick: (ReaderFilterType) -> Unit,
    onSelectedItemClick: () -> Unit,
    onSelectedItemDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedItem: ReaderFilterSelectedItem? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val blogSelected = selectedItem?.type == ReaderFilterType.BLOG
        val tagSelected = selectedItem?.type == ReaderFilterType.TAG
        val blogChipVisible = selectedItem == null || blogSelected
        val tagChipVisible = selectedItem == null || tagSelected

        val blogChipText: UiString = remember(selectedItem) {
            if (blogSelected) {
                selectedItem?.text ?: UiString.UiStringText("")
            } else {
                UiString.UiStringPluralRes(
                    zeroRes = R.string.reader_filter_chip_blog_zero,
                    oneRes = R.string.reader_filter_chip_blog_one,
                    otherRes = R.string.reader_filter_chip_blog_other,
                    count = followedBlogsCount,
                )
            }
        }

        val tagChipText: UiString = remember(selectedItem) {
            if (tagSelected) {
                selectedItem?.text ?: UiString.UiStringText("")
            } else {
                UiString.UiStringPluralRes(
                    zeroRes = R.string.reader_filter_chip_tag_zero,
                    oneRes = R.string.reader_filter_chip_tag_one,
                    otherRes = R.string.reader_filter_chip_tag_other,
                    count = followedTagsCount,
                )
            }
        }

        // blogs filter chip
        AnimatedVisibility(
            modifier = Modifier.clip(roundedShape),
            visible = blogChipVisible,
        ) {
            ReaderFilterChip(
                text = blogChipText,
                onClick = if (blogSelected) onSelectedItemClick else ({ onFilterClick(ReaderFilterType.BLOG) }),
                onDismissClick = if (blogSelected) onSelectedItemDismissClick else null,
                isSelectedItem = blogSelected,
            )
        }

        AnimatedVisibility(visible = blogChipVisible && tagChipVisible) {
            Spacer(Modifier.width(8.dp))
        }

        // tags filter chip
        AnimatedVisibility(
            modifier = Modifier.clip(roundedShape),
            visible = tagChipVisible,
        ) {
            ReaderFilterChip(
                text = tagChipText,
                onClick = if (tagSelected) onSelectedItemClick else ({ onFilterClick(ReaderFilterType.TAG) }),
                onDismissClick = if (tagSelected) onSelectedItemDismissClick else null,
                isSelectedItem = tagSelected,
            )
        }
    }
}

@Composable
fun ReaderFilterChip(
    text: UiString,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectedItem: Boolean = false,
    onDismissClick: (() -> Unit)? = null,
) {
    val backgroundColor by animateColorAsState(
        label = "ReaderFilterChip backgroundColor",
        targetValue = if (isSelectedItem) {
            MaterialTheme.colors.onSurface
        } else {
            MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
        }
    )

    val contentColor by animateColorAsState(
        label = "ReaderFilterChip contentColor",
        targetValue = if (isSelectedItem) {
            MaterialTheme.colors.surface
        } else {
            MaterialTheme.colors.onSurface
        }
    )

    val endPadding by animateDpAsState(
        label = "ReaderFilterChip endPadding",
        targetValue = if (onDismissClick != null) 12.dp else 24.dp
    )

    CompositionLocalProvider(
        LocalContentColor provides contentColor,
    ) {
        Row(
            modifier = modifier
                .background(
                    color = backgroundColor,
                    shape = roundedShape,
                )
                .clip(roundedShape)
                .clickable(onClick = onClick)
                .padding(
                    top = 8.dp,
                    bottom = 8.dp,
                    start = 24.dp,
                    end = endPadding,
                )
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                uiStringText(text),
                style = Material3Theme.typography.titleSmall,
                modifier = Modifier
                    .height(20.dp)
                    .align(Alignment.CenterVertically),
            )

            if (onDismissClick != null) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.dismiss),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(2.dp)
                        .clickable(
                            onClick = onDismissClick,
                            role = Role.Button,
                        ),
                )
            }
        }
    }
}

enum class ReaderFilterType {
    BLOG,
    TAG,
}

data class ReaderFilterSelectedItem(
    val text: UiString,
    val type: ReaderFilterType,
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ReaderFilterChipGroupPreview() {
    var selectedItem: ReaderFilterSelectedItem? by rememberSaveable { mutableStateOf(null) }

    AppThemeWithoutBackground {
        ReaderFilterChipGroup(
            modifier = Modifier.padding(8.dp),
            selectedItem = selectedItem,
            followedBlogsCount = 23,
            followedTagsCount = 41,
            onFilterClick = { type ->
                selectedItem = ReaderFilterSelectedItem(
                    text = UiString.UiStringText("Amazing ${type.name.lowercase()}"),
                    type = type,
                )
            },
            onSelectedItemClick = {
                selectedItem = null
            },
            onSelectedItemDismissClick = {
                selectedItem = null
            },
        )
    }
}
