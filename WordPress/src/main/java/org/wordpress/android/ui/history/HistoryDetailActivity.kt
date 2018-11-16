package org.wordpress.android.ui.history

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.history.HistoryListItem.Revision

class HistoryDetailActivity : AppCompatActivity() {
    companion object {
        const val KEY_HISTORY_DETAIL_FRAGMENT = "history_detail_fragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_detail_activity)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val extras = intent.extras
        val revision = extras.getParcelable(HistoryDetailContainerFragment.EXTRA_REVISION) as Revision
        val revisions = extras.getParcelableArrayList<Revision>(HistoryDetailContainerFragment.EXTRA_REVISIONS)

        var historyDetailContainerFragment = supportFragmentManager.findFragmentByTag(KEY_HISTORY_DETAIL_FRAGMENT)

        if (historyDetailContainerFragment == null) {
            historyDetailContainerFragment = HistoryDetailContainerFragment.newInstance(
                    revision,
                    revisions as ArrayList<Revision>
            )
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, historyDetailContainerFragment, KEY_HISTORY_DETAIL_FRAGMENT)
                    .commit()
        }
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
