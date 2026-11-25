@file:OptIn(ExperimentalMaterial3Api::class)

package org.wordpress.android.ui.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.prefs.AppPrefs

/** Where the bubble sits relative to its anchor. */
enum class FeatureTipPosition { Above, Below, Start, End }

// Maps to the experimental M3 type here so callers of FeatureTip don't need to opt in
private fun FeatureTipPosition.toTooltipAnchorPosition() = when (this) {
    FeatureTipPosition.Above -> TooltipAnchorPosition.Above
    FeatureTipPosition.Below -> TooltipAnchorPosition.Below
    FeatureTipPosition.Start -> TooltipAnchorPosition.Start
    FeatureTipPosition.End -> TooltipAnchorPosition.End
}

/**
 * Wraps [anchor] and, while [isVisible], shows a speech bubble pointing at it to explain a new feature.
 * Only the "Got it!" button closes the bubble; taps elsewhere pass through to the UI underneath.
 */
@Composable
fun FeatureTip(
    text: String,
    isVisible: Boolean,
    onGotIt: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    anchorPosition: FeatureTipPosition = FeatureTipPosition.Below,
    anchor: @Composable () -> Unit,
) {
    // A private mutex, since with M3's shared default any other tooltip's show() would cancel this one
    val state = rememberTooltipState(isPersistent = true, mutatorMutex = remember { MutatorMutex() })
    // A persistent tooltip's show() suspends until cancelled and dismisses on the way out,
    // so restarting this effect when isVisible turns false hides the bubble
    LaunchedEffect(isVisible) {
        if (isVisible) state.show()
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(anchorPosition.toTooltipAnchorPosition()),
        tooltip = {
            FeatureTipBubble(text = text, title = title, onGotIt = onGotIt)
        },
        state = state,
        modifier = modifier,
        onDismissRequest = {},
        enableUserInput = false,
        // hasAction is left false: with TalkBack on it makes the popup focusable and touch-modal, and since
        // onDismissRequest ignores Back and outside taps, the user would be trapped until they hit "Got it!"
        content = anchor,
    )
}

// Twice M3's default caret height. Created once since the shape has no equals() and RichTooltip remembers by it
private val FeatureTipCaretShape = TooltipDefaults.caretSize.let {
    TooltipDefaults.caretShape(it.copy(height = it.height * 2))
}

@Composable
private fun TooltipScope.FeatureTipBubble(text: String, title: String?, onGotIt: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    RichTooltip(
        caretShape = FeatureTipCaretShape,
        colors = TooltipDefaults.richTooltipColors(
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
        ),
    ) {
        // Everything goes in the text slot because RichTooltip start-aligns its title and action.
        // IntrinsicSize.Max sizes the bubble to its widest line instead of the full tooltip max width.
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .padding(top = 8.dp)
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(text = text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            TextButton(
                onClick = onGotIt,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.onPrimaryContainer),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.feature_tip_got_it))
            }
        }
    }
}

/**
 * Same as the stateless [FeatureTip], but shows the bubble until the user taps "Got it!" and
 * records that in [prefKey] so it's never shown again.
 */
@Composable
fun FeatureTip(
    prefKey: AppPrefs.PrefKey,
    text: String,
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
        onGotIt = {
            AppPrefs.setBoolean(prefKey, true)
            isVisible = false
        },
        modifier = modifier,
        title = title,
        anchorPosition = anchorPosition,
        anchor = anchor,
    )
}

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
                text = "Tap here to see the posts you've saved to read later.",
                isVisible = true,
                onGotIt = {},
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Bookmark, contentDescription = null)
                }
            }
        }
    }
}
