package org.wordpress.android.util.extensions

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

// TODO it might be safer bringing in the androidx.activity:activity-compose lib
fun Activity.setContent(content: @Composable () -> Unit) {
    val composeView = ComposeView(this).apply { setContent(content) }
    setContentView(composeView)
}
