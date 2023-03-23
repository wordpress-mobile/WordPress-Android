package org.wordpress.android.ui.stats.refresh.lists.widget.alltime

import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.databinding.StatsAllTimeWidgetConfigureActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

class StatsAllTimeWidgetConfigureActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(StatsAllTimeWidgetConfigureActivityBinding.inflate(layoutInflater)) {
            setContentView(root)

            setSupportActionBar(toolbar.toolbarMain)
        }
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
