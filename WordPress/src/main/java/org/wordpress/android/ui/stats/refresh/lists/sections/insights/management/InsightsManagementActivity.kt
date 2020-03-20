package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.R

class InsightsManagementActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.insights_management_activity)

        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
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
