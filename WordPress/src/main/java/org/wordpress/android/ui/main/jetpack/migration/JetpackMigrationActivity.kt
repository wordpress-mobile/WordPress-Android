package org.wordpress.android.ui.main.jetpack.migration

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.ActivityJetpackMigrationBinding

@AndroidEntryPoint
class JetpackMigrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(ActivityJetpackMigrationBinding.inflate(layoutInflater)) {
            setContentView(root)
            val showDeleteWpState = intent.getBooleanExtra(KEY_SHOW_DELETE_WP_STATE, false)
            val isOpenFromDeepLink = intent.getBooleanExtra(KEY_IS_OPEN_FROM_DEEP_LINK, false)
            if (savedInstanceState == null) {
                val fragment = JetpackMigrationFragment.newInstance(showDeleteWpState, isOpenFromDeepLink)
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit()
            }
        }
    }

    companion object {
        const val KEY_IS_OPEN_FROM_DEEP_LINK = "KEY_IS_OPEN_FROM_DEEP_LINK"
        private const val KEY_SHOW_DELETE_WP_STATE = "KEY_SHOW_DELETE_WP_STATE"
        fun createIntent(context: Context, showDeleteWpState: Boolean = false): Intent =
                Intent(context, JetpackMigrationActivity::class.java).apply {
                    putExtra(KEY_SHOW_DELETE_WP_STATE, showDeleteWpState)
                }
    }
}
