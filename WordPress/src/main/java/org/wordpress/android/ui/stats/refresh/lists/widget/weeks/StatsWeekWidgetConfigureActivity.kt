package org.wordpress.android.ui.stats.refresh.lists.widget.weeks

import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.databinding.StatsWeekViewsWidgetConfigureActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

class StatsWeekWidgetConfigureActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(StatsWeekViewsWidgetConfigureActivityBinding.inflate(layoutInflater)) {
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
