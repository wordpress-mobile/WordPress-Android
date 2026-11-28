@file:OptIn(ExperimentalMaterial3Api::class)

package org.wordpress.android.ui.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
    anchorPosition: FeatureTipPosition = FeatureTipPosition.Below,
    anchor: @Composable () -> Unit,
) {
    // A private mutex, since with M3's shared default any other tooltip's show() would cancel this one
    val state = rememberTooltipState(isPersistent = true, mutatorMutex = remember { MutatorMutex() })
    // The anchor's bounds in the window, or null until it's laid out and whenever it isn't shown at full size:
    // partly off screen, or shrunk by an ancestor's transform such as ViewPager2's page transformer mid-swipe.
    // That also keeps tips on ViewPager2's off-screen neighbouring pages hidden.
    var anchorBounds by remember { mutableStateOf<Rect?>(null) }
    val show = isVisible && anchorBounds != null
    // A persistent tooltip's show() suspends until cancelled and dismisses on the way out, so restarting this
    // effect hides the bubble. Keying it on the bounds too hides the bubble while its anchor moves (a scroll,
    // a page swipe, a bar sliding away) and brings it back once the anchor has been still for the show delay.
    LaunchedEffect(show, anchorBounds) {
        if (show) {
            delay(SHOW_DELAY_MS)
            state.show()
        }
    }
    val maxWidth = anchorBounds?.let { sideMaxWidth(anchorPosition, it) } ?: TooltipDefaults.richTooltipMaxWidth
    // The caller's modifier goes on this Box rather than TooltipBox, which puts it on an inner node where
    // parent data such as Modifier.weight would be lost
    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            val isFullSize = bounds.width >= coordinates.size.width - 1 &&
                bounds.height >= coordinates.size.height - 1
            anchorBounds = bounds.takeIf { isFullSize }
        }
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                anchorPosition.toTooltipAnchorPosition()
            ),
            tooltip = {
                FeatureTipBubble(text = text, maxWidth = maxWidth, onDismiss = onDismiss)
            },
            state = state,
            onDismissRequest = {},
            enableUserInput = false,
            // hasAction is left false: with TalkBack on it makes the popup focusable and touch-modal, and since
            // onDismissRequest ignores Back and outside taps, the user would be trapped until they hit close
            content = anchor,
        )
    }
}

/**
 * For [FeatureTipPosition.Start] and [FeatureTipPosition.End], the widest the bubble can be and still fit beside
 * [anchorBounds]. M3 flips a bubble that doesn't fit to the anchor's other side without keeping it on screen, and
 * a custom position provider would lose M3's caret placement, so the bubble is narrowed instead.
 */
@Composable
private fun sideMaxWidth(position: FeatureTipPosition, anchorBounds: Rect): Dp {
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val isOnRight = when (position) {
        FeatureTipPosition.End -> isLtr
        FeatureTipPosition.Start -> !isLtr
        else -> return TooltipDefaults.richTooltipMaxWidth
    }
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val space = if (isOnRight) windowWidth - anchorBounds.right else anchorBounds.left
    val available = with(LocalDensity.current) { space.toDp() } - TooltipAnchorSpacing
    return available.coerceAtMost(TooltipDefaults.richTooltipMaxWidth)
}

private const val SHOW_DELAY_MS = 500L

/** Plain-text version of the stateless [FeatureTip]. */
@Composable
fun FeatureTip(
    text: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    anchorPosition: FeatureTipPosition = FeatureTipPosition.Below,
    anchor: @Composable () -> Unit,
) = FeatureTip(AnnotatedString(text), isVisible, onDismiss, modifier, anchorPosition, anchor)

// Twice M3's default caret height. Created once since the shape has no equals() and RichTooltip remembers by it
private val FeatureTipCaretShape = TooltipDefaults.caretSize.let {
    TooltipDefaults.caretShape(it.copy(height = it.height * 2))
}

@Composable
private fun TooltipScope.FeatureTipBubble(
    text: AnnotatedString,
    maxWidth: Dp,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    RichTooltip(
        caretShape = FeatureTipCaretShape,
        maxWidth = maxWidth,
        colors = TooltipDefaults.richTooltipColors(
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
        ),
    ) {
        // The close button goes in the text slot too, since RichTooltip start-aligns its title and action slots.
        // IntrinsicSize.Max sizes the bubble to its widest line instead of the full tooltip max width.
        Box(modifier = Modifier.width(IntrinsicSize.Max)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                // Inset on both sides by the close button's overlap so the text stays centered and clear of it
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CloseButtonSize - CloseButtonEndOffset, vertical = 16.dp)
            )
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

// Mirrors M3's internal SpacingBetweenTooltipAndAnchor
private val TooltipAnchorSpacing = 4.dp

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
    anchorPosition: FeatureTipPosition = FeatureTipPosition.Below,
    anchor: @Composable () -> Unit,
) = FeatureTip(prefKey, AnnotatedString(text), modifier, anchorPosition, anchor)

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
