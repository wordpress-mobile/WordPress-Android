package org.wordpress.android.ui.history

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.history.HistoryListItem.Revision

class HistoryDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.history_activity)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val extras = intent.extras
        val revision = extras.getParcelable(HistoryDetailsContainerFragment.EXTRA_REVISION) as Revision
        val revisions = extras.getParcelableArrayList<Revision>(HistoryDetailsContainerFragment.EXTRA_REVISIONS)

        val fragment = HistoryDetailsContainerFragment.newInstance(revision, revisions as ArrayList<Revision>)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        AnalyticsTracker.track(Stat.REVISIONS_DETAIL_CANCELLED)
        finish()
        return true
    }

    override fun onBackPressed() {
        AnalyticsTracker.track(Stat.REVISIONS_DETAIL_CANCELLED)
        super.onBackPressed()
    }
}
