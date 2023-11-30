package org.wordpress.android.ui.bloganuary.learnmore

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.utils.UiString.UiStringRes
import androidx.compose.material.MaterialTheme as Material2Theme

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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Margin.ExtraMediumLarge.value)
                .padding(bottom = Margin.ExtraLarge.value)
        ) {
            Image(
                painter = painterResource(R.drawable.logo_bloganuary),
                colorFilter = ColorFilter.tint(Material2Theme.colors.onSurface),
                modifier = Modifier.width(140.dp),
                contentScale = ContentScale.Inside,
                contentDescription = stringResource(
                    R.string.bloganuary_dashboard_nudge_overlay_icon_content_description
                )
            )

            Spacer(Modifier.height(Margin.ExtraMediumLarge.value))

            // Title
            Text(
                stringResource(R.string.bloganuary_dashboard_nudge_overlay_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Margin.ExtraExtraMediumLarge.value))

            // Bullet points
            Column(
                verticalArrangement = Arrangement.spacedBy(Margin.ExtraMediumLarge.value),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Margin.ExtraMediumLarge.value),
            ) {
                OverlayContentItem(
                    iconRes = R.drawable.ic_dayone_24dp,
                    textRes = R.string.bloganuary_dashboard_nudge_overlay_text_one,
                )

                OverlayContentItem(
                    iconRes = R.drawable.ic_dayone_24dp,
                    textRes = R.string.bloganuary_dashboard_nudge_overlay_text_two,
                )

                OverlayContentItem(
                    iconRes = R.drawable.ic_dayone_24dp,
                    textRes = R.string.bloganuary_dashboard_nudge_overlay_text_three,
                )
            }

            Spacer(
                Modifier
                    .heightIn(min = Margin.ExtraLarge.value)
                    .weight(1f)
            )

            // Note
            Text(
                uiStringText(model.noteText),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = ContentAlpha.medium),
            )
        }

        Divider()

        Button(
            onClick = { onActionClick(model.action) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Margin.ExtraMediumLarge.value),
            elevation = null,
            contentPadding = PaddingValues(vertical = Margin.Large.value),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Material2Theme.colors.onSurface,
                contentColor = Material2Theme.colors.surface,
            ),
        ) {
            Text(stringResource(model.action.textRes))
        }
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
                    color = Material2Theme.colors.onSurface,
                    shape = CircleShape,
                ),
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Material2Theme.colors.surface),
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center)
            )
        }

        Spacer(Modifier.width(Margin.ExtraLarge.value))

        Text(
            stringResource(textRes),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}


@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun BloganuaryNudgeLearnMoreOverlayPreview() {
    AppTheme {
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
