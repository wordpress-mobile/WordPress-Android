package org.wordpress.android.ui.debug

import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.databinding.DebugSettingsActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

class DebugSettingsActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(DebugSettingsActivityBinding.inflate(layoutInflater).root)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
