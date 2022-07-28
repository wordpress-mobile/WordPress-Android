package org.wordpress.android.ui.activitylog.detail

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.databinding.ActivityLogDetailActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.RequestCodes

@AndroidEntryPoint
class ActivityLogDetailActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityLogDetailActivityBinding.inflate(layoutInflater)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCodes.RESTORE -> if (resultCode == RESULT_OK) onActivityResultForRestore(data)
            RequestCodes.BACKUP_DOWNLOAD -> if (resultCode == RESULT_OK) onActivityResultForBackupDownload(data)
        }
    }

    private fun onActivityResultForRestore(data: Intent?) {
        data?.putExtra(EXTRA_INNER_FLOW, EXTRA_RESTORE_FLOW)
        setResult(RESULT_OK, data)
        finish()
    }

    private fun onActivityResultForBackupDownload(data: Intent?) {
        data?.putExtra(EXTRA_INNER_FLOW, EXTRA_BACKUP_DOWNLOAD_FLOW)
        setResult(RESULT_OK, data)
        finish()
    }

    companion object {
        const val EXTRA_INNER_FLOW = "extra_inner_flow"
        const val EXTRA_RESTORE_FLOW = "extra_restore_inner_flow"
        const val EXTRA_BACKUP_DOWNLOAD_FLOW = "extra_backup_download_inner_flow"
    }
}
