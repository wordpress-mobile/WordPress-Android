package org.wordpress.android.ui.support

import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.databinding.SupportFormActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

class SupportFormActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SupportFormActivityBinding.inflate(layoutInflater).root)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}

