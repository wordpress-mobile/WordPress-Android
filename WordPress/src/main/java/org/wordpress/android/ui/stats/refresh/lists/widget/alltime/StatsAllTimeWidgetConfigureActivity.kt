package org.wordpress.android.ui.stats.refresh.lists.widget.alltime

import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.toolbar_main.*
import org.wordpress.android.R
import org.wordpress.android.ui.LocaleAwareActivity

class StatsAllTimeWidgetConfigureActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.stats_all_time_widget_configure_activity)

        setSupportActionBar(toolbar_main)
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
