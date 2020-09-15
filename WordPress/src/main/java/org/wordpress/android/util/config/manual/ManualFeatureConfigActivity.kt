package org.wordpress.android.util.config.manual

import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.manual_feature_config_fragment.*
import org.wordpress.android.R
import org.wordpress.android.ui.LocaleAwareActivity

class ManualFeatureConfigActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manual_feature_config_activity)

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
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
