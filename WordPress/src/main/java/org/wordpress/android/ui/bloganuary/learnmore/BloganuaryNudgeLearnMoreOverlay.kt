package org.wordpress.android.ui.bloganuary.learnmore

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
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
import org.wordpress.android.ui.utils.UiString.UiStringRes

private val contentIconForegroundColor: Color
    get() = AppColor.White

@Composable
private fun contentIconBackgroundColor(isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
    return if (isDarkTheme) {
        AppColor.Black
    } else {
        AppColor.White.copy(alpha = 0.18f)
    }
}

@Composable
private fun contentTextEmphasis(isDarkTheme: Boolean = isSystemInDarkTheme()): Float {
    return if (isDarkTheme) {
        0.4f // TODO verify this is correct
    } else {
        1f
    }
}

@Composable
fun BloganuaryNudgeLearnMoreOverlay(
    model: BloganuaryNudgeLearnMoreOverlayUiState,
    onActionClick: (BloganuaryNudgeLearnMoreOverlayAction) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        IconButton(
            onClick = onCloseClick,
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
                Image(
                    painter = painterResource(R.drawable.logo_bloganuary),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.width(180.dp),
                    contentScale = ContentScale.Inside,
                    contentDescription = stringResource(
                        R.string.bloganuary_dashboard_nudge_overlay_icon_content_description
                    )
                )

                Spacer(Modifier.height(Margin.ExtraMediumLarge.value))

                // Title
                Text(
                    stringResource(R.string.bloganuary_dashboard_nudge_overlay_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(Margin.ExtraExtraMediumLarge.value))

                // Bullet points
                OverlayContent(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .padding(horizontal = Margin.ExtraMediumLarge.value),
                )

                // min spacing
                Spacer(Modifier.height(Margin.ExtraLarge.value))
                Spacer(Modifier.weight(1f))

                // Note
                Text(
                    uiStringText(model.noteText),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.4f),
                )
            }
        }

        HorizontalDivider()

        Button(
            onClick = { onActionClick(model.action) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Margin.ExtraMediumLarge.value),
            elevation = null,
            contentPadding = PaddingValues(vertical = Margin.Large.value),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Text(stringResource(model.action.textRes))
        }
    }
}

@Composable
private fun OverlayContent(
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Margin.ExtraMediumLarge.value),
        modifier = modifier,
    ) {
        OverlayContentItem(
            iconRes = R.drawable.ic_bloganuary_learn_more_item_one,
            textRes = R.string.bloganuary_dashboard_nudge_overlay_text_one,
        )

        OverlayContentItem(
            iconRes = R.drawable.ic_bloganuary_learn_more_item_two,
            textRes = R.string.bloganuary_dashboard_nudge_overlay_text_two,
        )

        OverlayContentItem(
            iconRes = R.drawable.ic_bloganuary_learn_more_item_three,
            textRes = R.string.bloganuary_dashboard_nudge_overlay_text_three,
        )
    }
}

@Composable
private fun OverlayContentItem(
    iconRes: Int,
    textRes: Int,
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
                    color = contentIconBackgroundColor(),
                    shape = CircleShape,
                ),
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentIconForegroundColor),
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center)
            )
        }

        Spacer(Modifier.width(Margin.ExtraLarge.value))

        ContentAlphaProvider(contentTextEmphasis()) {
            Text(
                stringResource(textRes),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}


@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun BloganuaryNudgeLearnMoreOverlayPreview() {
    AppThemeM2 {
        BloganuaryNudgeLearnMoreOverlay(
            model = BloganuaryNudgeLearnMoreOverlayUiState(
                noteText = UiStringRes(
                    R.string.bloganuary_dashboard_nudge_overlay_note_prompts_enabled
                ),
                action = BloganuaryNudgeLearnMoreOverlayAction.DISMISS,
            ),
            onActionClick = {},
            onCloseClick = {},
        )
    }
}
