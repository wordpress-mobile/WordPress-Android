package org.wordpress.android.designsystem

import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.setContent

class DesignSystemActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            setContent {
                DesignSystemTheme(isSystemInDarkTheme()) {
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
        DesignSystemTheme(isSystemInDarkTheme()) {
            DesignSystem(onBackPressedDispatcher::onBackPressed)
        }
    }
}
