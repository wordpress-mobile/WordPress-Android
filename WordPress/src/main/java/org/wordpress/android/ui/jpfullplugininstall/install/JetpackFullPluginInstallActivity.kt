package org.wordpress.android.ui.jpfullplugininstall.install

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class JetpackFullPluginInstallActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
//                JetpackFullPluginInstallScreen()
            }
        }
    }

//    @Composable
//    private fun JetpackFullPluginInstallScreen() {
//
//    }
}
