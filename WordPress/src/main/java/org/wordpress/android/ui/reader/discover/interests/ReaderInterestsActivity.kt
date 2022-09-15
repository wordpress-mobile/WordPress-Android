package org.wordpress.android.ui.reader.discover.interests

import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.R
import org.wordpress.android.databinding.ReaderInterestsActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

class ReaderInterestsActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ReaderInterestsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMain)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            it.title = getString(R.string.reader_title_interests)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
    }
}
