package org.wordpress.android.stats

import androidx.compose.runtime.Composable

data class TabItem(
    val title: String,
    val screen: @Composable () -> Unit,
)

val tabs = listOf(
    TabItem(
        title =  "Traffic",
        screen = {
            TabScreen(
                content = "Traffic"
            )
        }
    ),
    TabItem(
        title = "Insights",
        screen = {
            TabScreen(
                content = "Insights"
            )
        }
    ),
    TabItem(
        title = "Subscribers",
        screen = {
            TabScreen(
                content = "Subscribers"
            )
        }
    ),
)
