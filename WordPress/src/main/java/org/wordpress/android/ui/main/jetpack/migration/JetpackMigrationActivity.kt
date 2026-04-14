package org.wordpress.android.ui.main.jetpack.migration

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.ActivityJetpackMigrationBinding
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.utils.PreMigrationDeepLinkData
import org.wordpress.android.util.extensions.getParcelableExtraCompat

@AndroidEntryPoint
class JetpackMigrationActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(ActivityJetpackMigrationBinding.inflate(layoutInflater)) {
            setContentView(root)
            if (savedInstanceState == null) {
                val deepLinkData = intent.getParcelableExtraCompat<PreMigrationDeepLinkData>(KEY_DEEP_LINK_DATA)
                val fragment = JetpackMigrationFragment.newInstance(deepLinkData)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
            }
        }
    }

    companion object {
        const val KEY_DEEP_LINK_DATA = "KEY_DEEP_LINK_DATA"
    }
}
