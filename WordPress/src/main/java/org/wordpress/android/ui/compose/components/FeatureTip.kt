@file:OptIn(ExperimentalMaterial3Api::class)

package org.wordpress.android.ui.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.delay
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.prefs.AppPrefs

// Maps to the experimental M3 type here so callers of FeatureTip don't need to opt in
private fun FeatureTipPosition.toTooltipAnchorPosition() = when (this) {
    FeatureTipPosition.Above -> TooltipAnchorPosition.Above
    FeatureTipPosition.Below -> TooltipAnchorPosition.Below
    FeatureTipPosition.Start -> TooltipAnchorPosition.Start
    FeatureTipPosition.End -> TooltipAnchorPosition.End
}

/**
 * Wraps [anchor] and, while [isVisible], shows a speech bubble pointing at it to explain a new feature.
 * Only its close button dismisses the bubble; taps elsewhere pass through to the UI underneath.
 * [text] can carry formatting, e.g. `AnnotatedString.fromHtml(stringResource(...))` or `buildAnnotatedString`;
 * there's a plain [String] overload for unformatted text.
 */
@Composable
fun FeatureTip(
    text: AnnotatedString,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    anchorPosition: FeatureTipPosition = FeatureTipPosition.Below,
    anchor: @Composable () -> Unit,
) {
    // A private mutex, since with M3's shared default any other tooltip's show() would cancel this one
    val state = rememberTooltipState(isPersistent = true, mutatorMutex = remember { MutatorMutex() })
    // Only shown while the screen is resumed. ViewPager2 keeps neighbouring pages composed at STARTED,
    // and their bubbles would otherwise pop up over the current page.
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val show = isVisible && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    // A persistent tooltip's show() suspends until cancelled and dismisses on the way out,
    // so restarting this effect when show turns false hides the bubble
    LaunchedEffect(show) {
        if (show) {
            delay(SHOW_DELAY_MS)
            state.show()
        }
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(anchorPosition.toTooltipAnchorPosition()),
        tooltip = {
            FeatureTipBubble(text = text, title = title, onDismiss = onDismiss)
        },
        state = state,
        modifier = modifier,
        onDismissRequest = {},
        enableUserInput = false,
        // hasAction is left false: with TalkBack on it makes the popup focusable and touch-modal, and since
        // onDismissRequest ignores Back and outside taps, the user would be trapped until they hit close
        content = anchor,
    )
}

private const val SHOW_DELAY_MS = 500L

/** Plain-text version of the stateless [FeatureTip]. */
@Composable
fun FeatureTip(
    text: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    anchorPosition: FeatureTipPosition = FeatureTipPosition.Below,
    anchor: @Composable () -> Unit,
) = FeatureTip(AnnotatedString(text), isVisible, onDismiss, modifier, title, anchorPosition, anchor)

// Twice M3's default caret height. Created once since the shape has no equals() and RichTooltip remembers by it
private val FeatureTipCaretShape = TooltipDefaults.caretSize.let {
    TooltipDefaults.caretShape(it.copy(height = it.height * 2))
}

@Composable
private fun TooltipScope.FeatureTipBubble(text: AnnotatedString, title: String?, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    RichTooltip(
        caretShape = FeatureTipCaretShape,
        colors = TooltipDefaults.richTooltipColors(
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
        ),
    ) {
        // Everything goes in the text slot because RichTooltip start-aligns its title.
        // IntrinsicSize.Max sizes the bubble to its widest line instead of the full tooltip max width.
        Box(modifier = Modifier.width(IntrinsicSize.Max)) {
            // Inset on both sides by the close button's overlap so the text stays centered and clear of it
            Column(
                modifier = Modifier.padding(
                    horizontal = CloseButtonSize - CloseButtonEndOffset,
                    vertical = 16.dp
                )
            ) {
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Pushed into RichTooltip's end padding so it sits in the bubble's corner. Compose still gives
            // it a 48dp touch target despite the smaller layout size.
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = CloseButtonEndOffset)
                    .size(CloseButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private val CloseButtonSize = 32.dp

// Mirrors M3's internal RichTooltipHorizontalPadding, which the close button is pushed into
private val RichTooltipHorizontalPadding = 16.dp

// Leaves a 4dp margin between the close button and the bubble's end edge
private val CloseButtonEndOffset = RichTooltipHorizontalPadding - 4.dp

/**
 * Same as the stateless [FeatureTip], but shows the bubble until the user closes it and
 * records that in [prefKey] so it's never shown again.
 */
@Composable
fun FeatureTip(
    prefKey: AppPrefs.PrefKey,
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    title: String? = null,
    anchorPosition: FeatureTipPosition = FeatureTipPosition.Below,
    anchor: @Composable () -> Unit,
) {
    val wasShown = remember { AppPrefs.getBoolean(prefKey, false) }
    if (wasShown) {
        // Skip the tooltip machinery entirely for the common case of a tip that's already been seen
        Box(modifier) { anchor() }
        return
    }
    var isVisible by remember { mutableStateOf(true) }
    FeatureTip(
        text = text,
        isVisible = isVisible,
        onDismiss = {
            AppPrefs.setBoolean(prefKey, true)
            isVisible = false
        },
        modifier = modifier,
        title = title,
        anchorPosition = anchorPosition,
        anchor = anchor,
    )
}

/** Plain-text version of the self-persisting [FeatureTip]. */
@Composable
fun FeatureTip(
    prefKey: AppPrefs.PrefKey,
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    anchorPosition: FeatureTipPosition = FeatureTipPosition.Below,
    anchor: @Composable () -> Unit,
) = FeatureTip(prefKey, AnnotatedString(text), modifier, title, anchorPosition, anchor)

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FeatureTipPreview() {
    AppThemeM3 {
        Box(
            modifier = Modifier
                .height(240.dp)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            FeatureTip(
                title = "Saved posts",
                text = AnnotatedString.fromHtml("<b>New!</b> Tap here to see the posts you've saved to read later."),
                isVisible = true,
                onDismiss = {},
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Bookmark, contentDescription = null)
                }
            }
        }
    }
}
