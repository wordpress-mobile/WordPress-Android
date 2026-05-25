package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

private val HORIZONTAL_PADDING = 24.dp
private val HANDLE_WIDTH = 32.dp
private val HANDLE_HEIGHT = 4.dp
private val HANDLE_TOP_MARGIN = 4.dp
private val TITLE_TOP_SPACE = 24.dp
private val TITLE_BODY_SPACE = 8.dp
private val BUTTONS_TOP_SPACE = 24.dp
private val BOTTOM_SPACE = 16.dp
private val BUTTON_GAP = 8.dp
private const val HANDLE_ALPHA = 0.38f

@Composable
fun GutenbergKitAnnouncementScreen(
    onActivate: () -> Unit,
    onMaybeLater: () -> Unit,
    onLearnMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING)
    ) {
        BottomSheetHandle(modifier = Modifier.padding(top = HANDLE_TOP_MARGIN))

        Spacer(modifier = Modifier.height(TITLE_TOP_SPACE))
        Text(
            text = stringResource(R.string.gutenberg_kit_announcement_title),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(TITLE_BODY_SPACE))
        Text(
            text = buildBodyWithLearnMore(onLearnMore),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(BUTTONS_TOP_SPACE))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BUTTON_GAP),
        ) {
            TextButton(
                onClick = onMaybeLater,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.gutenberg_kit_announcement_maybe_later))
            }
            Button(
                onClick = onActivate,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.gutenberg_kit_announcement_activate))
            }
        }

        Spacer(modifier = Modifier.height(BOTTOM_SPACE))
    }
}

@Composable
private fun buildBodyWithLearnMore(onLearnMore: () -> Unit) = buildAnnotatedString {
    val learnMoreText = stringResource(R.string.gutenberg_kit_announcement_learn_more)
    val body = stringResource(R.string.gutenberg_kit_announcement_body, learnMoreText)
    val linkStart = body.indexOf(learnMoreText)
    val link = LinkAnnotation.Clickable(
        tag = "learn_more",
        styles = TextLinkStyles(
            style = SpanStyle(color = MaterialTheme.colorScheme.primary),
        ),
        linkInteractionListener = { onLearnMore() },
    )
    append(body.substring(0, linkStart))
    withLink(link) { append(learnMoreText) }
    val suffixStart = linkStart + learnMoreText.length
    if (suffixStart < body.length) append(body.substring(suffixStart))
}

@Composable
private fun BottomSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = HANDLE_WIDTH, height = HANDLE_HEIGHT)
                .alpha(HANDLE_ALPHA)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(HANDLE_HEIGHT / 2),
                ),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GutenbergKitAnnouncementScreenPreview() {
    AppThemeM3 {
        GutenbergKitAnnouncementScreen(
            onActivate = {},
            onMaybeLater = {},
            onLearnMore = {},
        )
    }
}
