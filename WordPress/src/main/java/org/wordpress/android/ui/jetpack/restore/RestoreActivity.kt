package org.wordpress.android.ui.jetpack.restore

import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.restore_activity.*
import org.wordpress.android.R
import org.wordpress.android.ui.LocaleAwareActivity

class RestoreActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.restore_activity)

        setSupportActionBar(toolbar_main)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return false
    }
}
