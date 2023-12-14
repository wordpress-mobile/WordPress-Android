package org.wordpress.android.ui.jetpack.restore

import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.databinding.RestoreActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity
import android.R as AndroidR

class RestoreActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(RestoreActivityBinding.inflate(layoutInflater)) {
            setContentView(root)

            setSupportActionBar(toolbarMain)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == AndroidR.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }
}
