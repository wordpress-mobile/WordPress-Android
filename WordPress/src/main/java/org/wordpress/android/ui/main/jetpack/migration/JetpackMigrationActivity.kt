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
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, JetpackMigrationFragment.newInstance(showDeleteWpState))
                    .commit()
        }
    }

    companion object {
        private const val KEY_SHOW_DELETE_WP_STATE = "KEY_SHOW_DELETE_WP_STATE"
        fun createIntent(context: Context, showDeleteWpState: Boolean = false): Intent =
                Intent(context, JetpackMigrationActivity::class.java).apply {
                    putExtra(KEY_SHOW_DELETE_WP_STATE, showDeleteWpState)
                }
    }
}
