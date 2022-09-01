package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.util.extensions.isOdd

val texts = listOf(
        "Update a plugin",
        "Build a site",
        "Write a blog",
        "Watch your stats",
        "Check notifications",
        "Respond to comments",
        "Restore a backup",
        "Search for plugins",
        "Share on Facebook",
        "Fix a security issue",
        "Post a photo",
        "Add an author",
).reversed()

data class LargeTextUiState(
    override val id: Int,
    val text: String,
) : AutoScrollingListItem

// Duplicating the list fixes a strange issue where the autoscroll stops for smaller font sizes,
// when almost the entire list is visible initially on the screen.
val largeTextsUiState = (texts + texts).mapIndexed { index, value ->  LargeTextUiState(index, value) }

private val Divider = @Composable {
    Spacer(modifier = Modifier.height(2.dp))
}

@Composable
fun LargeTexts(
    items: List<LargeTextUiState> = largeTextsUiState,
    modifier: Modifier = Modifier,
) {
    AutoScrollingLazyColumn(
            items = items,
            itemDivider = Divider,
            modifier = modifier,
    ) {
        LargeText(
                text = it.text,
                color = when (items.indexOf(it).isOdd) {
                    true -> colorResource(R.color.text_color_jetpack_login_feature_odd)
                    false -> colorResource(R.color.text_color_jetpack_login_feature_even)
                }
        )
    }
}

@Composable
fun LargeText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val fontSize by remember { mutableStateOf(43.sp) }
    val lineHeight by remember { mutableStateOf(fontSize * 0.95) }

    Text(
            text = text,
            style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    lineHeight = lineHeight,
            ),
            color = color,
            modifier = modifier
    )
}
