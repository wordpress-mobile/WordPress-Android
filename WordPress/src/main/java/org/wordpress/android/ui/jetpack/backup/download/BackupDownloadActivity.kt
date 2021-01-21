package org.wordpress.android.ui.jetpack.backup.download

import android.R.id
import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.backup_download_activity.*
import org.wordpress.android.R
import org.wordpress.android.ui.LocaleAwareActivity

class BackupDownloadActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.backup_download_activity)

        setSupportActionBar(toolbar_main)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
