package org.wordpress.android.ui.activitylog.list

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.toolbar_main.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.posts.BasicFragmentDialog
import org.wordpress.android.util.BackupFeatureConfig
import org.wordpress.android.viewmodel.activitylog.ACTIVITY_LOG_REWIND_ID_KEY
import javax.inject.Inject

class ActivityLogListActivity : LocaleAwareActivity(),
        BasicFragmentDialog.BasicDialogPositiveClickInterface,
        BasicFragmentDialog.BasicDialogNegativeClickInterface {
    @Inject lateinit var backupFeatureConfig: BackupFeatureConfig
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        setContentView(R.layout.activity_log_list_activity)

        setSupportActionBar(toolbar_main)
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
        if (requestCode == RequestCodes.ACTIVITY_LOG_DETAIL) {
            data?.getStringExtra(ACTIVITY_LOG_REWIND_ID_KEY)?.let {
                passRewindConfirmation(it)
            }
        }
    }

    override fun onPositiveClicked(instanceTag: String) {
        passRewindConfirmation(instanceTag)
    }

    override fun onNegativeClicked(instanceTag: String) {
        // todo: annmarie - this is temporary to expose backup feature
        if (backupFeatureConfig.isEnabled()) {
            ActivityLauncher.showBackupDownload(this)
        }
    }

    private fun passRewindConfirmation(rewindId: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is ActivityLogListFragment) {
            fragment.onRewindConfirmed(rewindId)
        }
    }
}
