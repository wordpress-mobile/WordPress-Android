package org.wordpress.android.ui.main.jetpack.migration

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.ActivityJetpackMigrationBinding
import org.wordpress.android.ui.utils.PreMigrationDeepLinkData
import org.wordpress.android.util.extensions.getParcelableExtraCompat

@AndroidEntryPoint
class JetpackMigrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(ActivityJetpackMigrationBinding.inflate(layoutInflater)) {
            setContentView(root)
            if (savedInstanceState == null) {
                val showDeleteWpState = intent.getBooleanExtra(KEY_SHOW_DELETE_WP_STATE, false)
                val deepLinkData = intent.getParcelableExtraCompat<PreMigrationDeepLinkData>(KEY_DEEP_LINK_DATA)
                val fragment = JetpackMigrationFragment.newInstance(showDeleteWpState, deepLinkData)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
            }
        }
    }

    companion object {
        const val KEY_DEEP_LINK_DATA = "KEY_DEEP_LINK_DATA"
        private const val KEY_SHOW_DELETE_WP_STATE = "KEY_SHOW_DELETE_WP_STATE"
        fun createIntent(context: Context, showDeleteWpState: Boolean = false): Intent =
            Intent(context, JetpackMigrationActivity::class.java).apply {
                putExtra(KEY_SHOW_DELETE_WP_STATE, showDeleteWpState)
            }
    }
}
