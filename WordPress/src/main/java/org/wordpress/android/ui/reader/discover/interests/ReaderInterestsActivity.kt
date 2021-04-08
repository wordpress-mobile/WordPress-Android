package org.wordpress.android.ui.reader.discover.interests

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.databinding.ReaderInterestsActivityBinding

class ReaderInterestsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ReaderInterestsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMain)
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
