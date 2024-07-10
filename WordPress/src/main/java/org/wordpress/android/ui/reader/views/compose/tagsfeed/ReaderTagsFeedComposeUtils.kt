package org.wordpress.android.ui.reader.views.compose.tagsfeed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
            val localConfiguration = LocalConfiguration.current
            val screenWidth = remember(localConfiguration) {
                localConfiguration.screenWidthDp.dp
            }
            return min((screenWidth * POST_ITEM_WIDTH_PERCENTAGE), POST_ITEM_MAX_WIDTH)
        }
}
