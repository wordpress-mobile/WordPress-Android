package org.wordpress.android.ui.stats.refresh.lists.widget.minified

import android.os.Bundle
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.databinding.StatsMinifiedWidgetConfigureActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity
import android.R as AndroidR

@AndroidEntryPoint
class StatsMinifiedWidgetConfigureActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(StatsMinifiedWidgetConfigureActivityBinding.inflate(layoutInflater)) {
            setContentView(root)

            setSupportActionBar(toolbar.toolbarMain)
        }
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == AndroidR.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
