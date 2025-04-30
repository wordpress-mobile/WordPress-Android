package org.wordpress.android.ui.reader.views.compose.tagsfeed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp

object ReaderTagsFeedComposeUtils {
    const val LOADING_POSTS_COUNT = 5

    const val POST_ITEM_TITLE_MAX_LINES = 2
    val POST_ITEM_IMAGE_SIZE = 64.dp
    private val POST_ITEM_HEIGHT = 150.sp // use SP to scale with text size, which is the main content of the item
    private val POST_ITEM_MAX_WIDTH = 320.dp
    private const val POST_ITEM_WIDTH_PERCENTAGE = 0.8f

    val PostItemHeight: Dp
        @Composable
        get() {
            with(LocalDensity.current) {
                return POST_ITEM_HEIGHT.toDp()
            }
        }

    val PostItemWidth: Dp
        @Composable
        get() {
            var containerWidthInPixels by remember { mutableStateOf(0) }
            val windowInfo = LocalWindowInfo.current
            val localDensity = LocalDensity.current

            // Update the width in pixels only when it changes
            if (windowInfo.containerSize.width != containerWidthInPixels) {
                containerWidthInPixels = windowInfo.containerSize.width
            }

            val screenWidth: Dp = with(localDensity) {
                containerWidthInPixels.toDp()
            }
            return min((screenWidth * POST_ITEM_WIDTH_PERCENTAGE), POST_ITEM_MAX_WIDTH)
        }
}
