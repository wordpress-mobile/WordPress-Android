package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.AutoScrollingLazyColumn
import org.wordpress.android.ui.compose.components.AutoScrollingListItem
import org.wordpress.android.ui.compose.components.AUTOSCROLL_DELTA_PX
import org.wordpress.android.util.extensions.isOdd

private data class LargeTextsItem(
    override val id: Int,
    val content: @Composable () -> Unit,
) : AutoScrollingListItem

@Composable
private fun largeTextItems(): List<LargeTextsItem> {
    return (0..2).map { index ->
        LargeTextsItem(index) {
            LargeTexts()
        }
    }
}

@Composable
fun AutoScrollingText(
    modifier: Modifier = Modifier,
) {
    val listItems = largeTextItems()
    val scrollBy = remember { mutableStateOf(AUTOSCROLL_DELTA_PX) }
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = listItems.size / 2)

    AutoScrollingLazyColumn(
            lazyListState = lazyListState,
            items = listItems,
            scrollBy = scrollBy,
            modifier = modifier,
    ) {
        it.content()
    }
}

@Composable
fun LargeTexts() {
    val texts = stringArrayResource(R.array.login_prologue_revamped_jetpack_feature_texts)

    texts.forEachIndexed { index, text ->
        LargeText(
                text = text,
                color = when (index.isOdd) {
                    true -> colorResource(R.color.text_color_jetpack_login_feature_odd)
                    false -> colorResource(R.color.text_color_jetpack_login_feature_even)
                }
        )
        Spacer(modifier = Modifier.height(2.dp))
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
