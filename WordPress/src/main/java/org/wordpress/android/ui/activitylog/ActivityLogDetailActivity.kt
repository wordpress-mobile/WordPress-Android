package org.wordpress.android.ui.activitylog

import android.os.Bundle
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.toolbar.*
import org.wordpress.android.R

private const val ACTIVITY_LOG_DETAIL_FRAGMENT_TAG = "activity_log_detail_fragment_tag"

class ActivityLogDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_log_activity)

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        showDetailFragment()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDetailFragment() {
        if (fragmentManager.findFragmentByTag(ACTIVITY_LOG_DETAIL_FRAGMENT_TAG) == null) {
            supportFragmentManager.beginTransaction()
                    .add(
                            R.id.fragment_container,
                            ActivityLogDetailFragment.newInstance(),
                            ACTIVITY_LOG_DETAIL_FRAGMENT_TAG
                    )
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit()
        }
    }
}
