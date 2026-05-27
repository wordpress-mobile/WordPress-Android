package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

private val HORIZONTAL_PADDING = 24.dp
private val PARAGRAPH_SPACE = 8.dp
private val BUTTONS_TOP_SPACE = 24.dp
private val BUTTON_GAP = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
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
            .navigationBarsPadding()
            .padding(horizontal = HORIZONTAL_PADDING)
    ) {
        BottomSheetDefaults.DragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))

        Text(
            text = stringResource(R.string.gutenberg_kit_announcement_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(PARAGRAPH_SPACE))
        Text(
            text = stringResource(R.string.gutenberg_kit_announcement_body_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(PARAGRAPH_SPACE))
        Text(
            text = buildDeadlineWithLearnMore(onLearnMore),
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
    }
}

@Composable
private fun buildDeadlineWithLearnMore(onLearnMore: () -> Unit) = buildAnnotatedString {
    val learnMoreText = stringResource(R.string.gutenberg_kit_announcement_learn_more)
    val body = stringResource(R.string.gutenberg_kit_announcement_body_deadline, learnMoreText)
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
