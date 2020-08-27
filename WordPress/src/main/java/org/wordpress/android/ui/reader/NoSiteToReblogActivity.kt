package org.wordpress.android.ui.reader

import android.R.id
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.reader_no_site_to_reblog.noSiteToReblogView
import kotlinx.android.synthetic.main.toolbar_main.*
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher

/*
 * Serves as an intermediate screen where the user is informed that a site is needed for the reblog action
 */
class NoSiteToReblogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reader_no_site_to_reblog)

        setSupportActionBar(toolbar_main)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        noSiteToReblogView.button.setOnClickListener {
            ActivityLauncher.viewMySiteInNewStack(this@NoSiteToReblogActivity)
            setResult(RESULT_OK)
            finish()
            overridePendingTransition(0, 0)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
