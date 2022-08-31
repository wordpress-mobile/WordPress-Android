package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.unit.Margin
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
)

private val Divider = @Composable {
    Spacer(modifier = Modifier.height(Margin.Small.value))
}

@Composable
fun LargeTexts(
    items: List<String> = texts,
    modifier: Modifier = Modifier,
) {
    AutoScrollingLazyColumn(
            items = items,
            modifier = modifier,
            divider = Divider,
    ) {
        LargeText(
                text = it,
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
    Text(
            text = text,
            style = TextStyle(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    lineHeight = 36.sp, // 38 * 0,95
            ),
            color = color,
            modifier = modifier
    )
}
