package org.wordpress.android.ui.activitylog.list

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.toolbar.*
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.posts.BasicFragmentDialog
import org.wordpress.android.viewmodel.activitylog.ACTIVITY_LOG_REWIND_ID_KEY

class ActivityLogListActivity : AppCompatActivity(), BasicFragmentDialog.BasicDialogPositiveClickInterface,
        BasicFragmentDialog.BasicDialogNegativeClickInterface {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_log_list_activity)

        setSupportActionBar(toolbar)
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
    }

    private fun passRewindConfirmation(rewindId: String) {
        val fragment = supportFragmentManager.findFragmentById(id.fragment_container)
        if (fragment is ActivityLogListFragment) {
            fragment.onRewindConfirmed(rewindId)
        }
    }
}
