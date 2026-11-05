package org.wordpress.android.ui.localcontent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.setContent

/** Lists the kinds of content saved on the device, each opening its own list of working copies. */
class LocalContentActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppThemeM3 {
                LocalContentScreen(
                    onKindClick = { kind -> startActivity(WorkingCopyListActivity.createIntent(this, kind)) },
                    onNavigateBack = onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, LocalContentActivity::class.java)
    }
}
