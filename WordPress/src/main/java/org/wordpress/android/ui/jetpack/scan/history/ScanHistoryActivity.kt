package org.wordpress.android.ui.jetpack.scan.history

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.toolbar_main.*
import org.wordpress.android.R

class ScanHistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.scan_history_activity)

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
