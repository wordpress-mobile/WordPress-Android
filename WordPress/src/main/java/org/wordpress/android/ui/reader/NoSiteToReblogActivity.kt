package org.wordpress.android.ui.reader

import android.R.id
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.databinding.ReaderNoSiteToReblogBinding
import org.wordpress.android.ui.ActivityLauncher

/*
 * Serves as an intermediate screen where the user is informed that a site is needed for the reblog action
 */
class NoSiteToReblogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ReaderNoSiteToReblogBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setSupportActionBar(binding.includeToolbarMain.toolbarMain)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.noSiteToReblogView.button.setOnClickListener {
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
