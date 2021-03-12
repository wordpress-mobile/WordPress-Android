package org.wordpress.android.util.config.manual

import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.manual_feature_config_fragment.*
import org.wordpress.android.databinding.ManualFeatureConfigActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

class ManualFeatureConfigActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(ManualFeatureConfigActivityBinding.inflate(layoutInflater)) {
            setContentView(root)

            setSupportActionBar(toolbar)
            supportActionBar?.let {
                it.setHomeButtonEnabled(true)
                it.setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
