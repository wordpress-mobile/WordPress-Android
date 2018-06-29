package org.wordpress.android.ui.activitylog.detail

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.toolbar.*
import org.wordpress.android.R
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface

class ActivityLogDetailActivity : AppCompatActivity(), BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_log_detail_activity)

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

    override fun onPositiveClicked(instanceTag: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is ActivityLogDetailFragment) {
            fragment.onRewindConfirmed(instanceTag)
        }
    }

    override fun onNegativeClicked(instanceTag: String) {
    }
}
