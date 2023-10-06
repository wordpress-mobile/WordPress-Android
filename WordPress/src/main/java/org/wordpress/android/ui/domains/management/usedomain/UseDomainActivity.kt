package org.wordpress.android.ui.domains.management.usedomain

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.domains.management.usedomain.composable.UseDomainScreen

@AndroidEntryPoint
class UseDomainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                UseDomainScreen()
            }
        }
    }
}
