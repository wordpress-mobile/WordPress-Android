package org.wordpress.android.util.config.manual

import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.databinding.ManualFeatureConfigActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

class ManualFeatureConfigActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ManualFeatureConfigActivityBinding.inflate(layoutInflater).root)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
