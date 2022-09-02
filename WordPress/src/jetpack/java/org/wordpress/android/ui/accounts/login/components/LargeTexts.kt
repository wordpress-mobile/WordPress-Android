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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.AutoScrollingLazyColumn
import org.wordpress.android.ui.compose.components.AutoScrollingListItem
import org.wordpress.android.util.extensions.isOdd

private data class LargeTextItem(
    override val id: Int,
    val text: String,
) : AutoScrollingListItem

@Composable
fun LargeTexts(
    modifier: Modifier = Modifier,
) {
    val featureTexts = stringArrayResource(R.array.login_prologue_revamped_jetpack_feature_texts).reversed()

    /**
     * Duplicating the list fixes a strange issue where the autoscroll stops for smaller font sizes,
     * when almost the entire list is visible initially on the screen.
     */
    val listItems = (featureTexts + featureTexts).mapIndexed(::LargeTextItem)

    AutoScrollingLazyColumn(
            items = listItems,
            itemDivider = { Spacer(modifier = Modifier.height(2.dp)) },
            modifier = modifier,
    ) {
        LargeText(
                text = it.text,
                color = when (listItems.indexOf(it).isOdd) {
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
