package org.wordpress.android.ui.jetpackconnection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class JetpackConnectionActivity : BaseAppCompatActivity() {
    private val viewModel: JetpackConnectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JetpackConnectionScreen(
                viewModel = viewModel,
                onCloseClick = viewModel::onCloseClick
            )
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context) =
            Intent(context, JetpackConnectionActivity::class.java)
    }
}
