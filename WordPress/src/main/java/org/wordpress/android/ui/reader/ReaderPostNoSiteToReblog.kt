package org.wordpress.android.ui.reader

import android.R.id
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.reader_no_site_to_reblog.*
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.R

/*
 * Serves as an intermediate screen where the user is informed that a site is needed for the reblog action
 */
class ReaderPostNoSiteToReblog : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reader_no_site_to_reblog)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        no_site_to_reblog_view.button.setOnClickListener {
            setResult(RESULT_OK)
            finish()
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
