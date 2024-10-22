package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ContentAlphaProvider
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.utils.UiString

private val contentIconForegroundColor: Color
    get() = AppColor.White

private val contentIconBackgroundColor: Color
    @Composable get() = if (MaterialTheme.colors.isLight) {
        AppColor.Black
    } else {
        AppColor.White.copy(alpha = 0.18f)
    }

private val contentTextEmphasis: Float
    @Composable get() = if (MaterialTheme.colors.isLight) {
        1f
    } else {
        ContentAlpha.medium
    }

@Composable
fun PostResolutionOverlay(
    uiState: PostResolutionOverlayUiState?,
    modifier: Modifier = Modifier
) {
    if (uiState == null) return
    Column(modifier) {
        IconButton(
            onClick = uiState.closeClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.label_close_button),
            )
        }

        Spacer(
            Modifier
                .requiredHeightIn(
                    min = Margin.Medium.value,
                    max = Margin.ExtraExtraMediumLarge.value
                )
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Margin.ExtraMediumLarge.value)
                    .padding(bottom = Margin.ExtraLarge.value)
            ) {
                // Title
                Text(
                    stringResource(uiState.titleResId),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(Margin.ExtraLarge.value))

                Text(
                    stringResource(uiState.bodyResId),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(
                            start = Margin.ExtraMediumLarge.value,
                            end = Margin.ExtraMediumLarge.value
                        ),
                    textAlign = TextAlign.Center,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = ContentAlpha.medium),
                )

                Spacer(Modifier.height(Margin.ExtraExtraMediumLarge.value))

                // Device information
                OverlayContent(
                    items = uiState.content,
                    onSelected = uiState.onSelected,
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .padding(horizontal = Margin.ExtraMediumLarge.value),
                )

                // min spacing
                Spacer(Modifier.height(Margin.ExtraLarge.value))
                Spacer(Modifier.weight(1f))
            }
        }

        Divider()

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { uiState.cancelClick() },
                modifier = Modifier
                    .weight(1f)
                    .padding(Margin.ExtraMediumLarge.value),
                elevation = null,
                contentPadding = PaddingValues(vertical = Margin.Large.value),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.onSurface,
                    contentColor = MaterialTheme.colors.surface,
                ),
            ) {
                Text(text = stringResource(R.string.cancel))
            }
            Button(
                onClick = { uiState.confirmClick() },
                enabled = uiState.actionEnabled,
                modifier = Modifier
                    .weight(1f)
                    .padding(Margin.ExtraMediumLarge.value),
                elevation = null,
                contentPadding = PaddingValues(vertical = Margin.Large.value),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.onSurface,
                    contentColor = MaterialTheme.colors.surface,
                ),
            ) {
                Text(text = stringResource(R.string.confirm))
            }
        }
    }
}

@Composable
private fun OverlayContent(
    items: List<ContentItem>,
    onSelected: (ContentItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Margin.ExtraMediumLarge.value),
        modifier = modifier,
    ) {
        items.forEach { item ->
            OverlayContentItem(
                item = item,
                onSelected = onSelected
            )
        }
    }
}

@Composable
private fun OverlayContentItem(
    item: ContentItem,
    onSelected: (ContentItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = contentIconBackgroundColor,
                    shape = CircleShape,
                ),
        ) {
            Image(
                painter = painterResource(item.iconResId),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentIconForegroundColor),
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center)
            )
        }

        Spacer(Modifier.width(Margin.ExtraLarge.value))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = Margin.ExtraLarge.value)
        ) {
            ContentAlphaProvider(contentTextEmphasis) {
                Text(
                    stringResource(item.headerResId),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            ContentAlphaProvider(contentTextEmphasis) {
                Text(
                    uiStringText(item.dateLine),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        Checkbox(
            checked = item.isSelected,
            onCheckedChange = { isChecked ->
                onSelected(item.copy(isSelected = isChecked))
            },
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PostResolutionOverlayPreview() {
    AppThemeM2 {
        PostResolutionOverlay(
            uiState = PostResolutionOverlayUiState(
                titleResId = R.string.dialog_post_conflict_title,
                bodyResId = R.string.dialog_post_conflict_body,
                actionEnabled = false,
                confirmClick = {},
                closeClick = {},
                cancelClick = {},
                onSelected = {},
                content = listOf(
                    ContentItem(
                        headerResId = R.string.dialog_post_conflict_current_device,
                        dateLine = UiString.UiStringText("Thursday, Mar 4, 2024 1:00 PM"),
                        isSelected = true,
                        id = ContentItemType.LOCAL_DEVICE
                    ),
                    ContentItem(
                        headerResId = R.string.dialog_post_conflict_another_device,
                        dateLine = UiString.UiStringText("Friday, Mar 4, 2024 11:00 AM"),
                        isSelected = false,
                        id = ContentItemType.OTHER_DEVICE
                    )
                ),
            )
        )
    }
}
