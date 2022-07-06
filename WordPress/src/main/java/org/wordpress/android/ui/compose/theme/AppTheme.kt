package org.wordpress.android.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import org.wordpress.android.BuildConfig
import org.wordpress.android.ui.compose.theme.color.JetpackColors
import org.wordpress.android.ui.compose.theme.color.WordPressColors

/**
 * Project's base theme.
 */
@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val themeColors = if (BuildConfig.IS_JETPACK_APP) JetpackColors() else WordPressColors()
    MaterialTheme(
            colors = if (isDarkTheme) themeColors.dark() else themeColors.light(),
            content = content
    )
}
