package org.wordpress.android.ui.jetpack.backup.download

import android.R.id
import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.databinding.BackupDownloadActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

class BackupDownloadActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(BackupDownloadActivityBinding.inflate(layoutInflater)) {
            setContentView(root)

            setSupportActionBar(toolbarMain)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
