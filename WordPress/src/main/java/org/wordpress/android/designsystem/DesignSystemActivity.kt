package org.wordpress.android.designsystem

import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.util.extensions.setContent

class DesignSystemActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            setContent {
                val colors = if (isSystemInDarkTheme()) darkColors() else lightColors()
                DesignSystemTheme(colors) {
                    DesignSystem(onBackPressedDispatcher::onBackPressed)
            }
        }
    }

    @Preview(name = "Light Mode")
    @Preview(
        uiMode = Configuration.UI_MODE_NIGHT_YES,
        showBackground = true,
        name = "Dark Mode"
    )
    @Composable
    fun PreviewDesignSystemActivity() {
        val colors = if (isSystemInDarkTheme()) darkColors() else lightColors()
        DesignSystemTheme(colors) {
            DesignSystem(onBackPressedDispatcher::onBackPressed)
        }
    }
}
