package org.wordpress.android.ui.history

import android.os.Bundle
import androidx.activity.addCallback
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.HistoryDetailActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.history.HistoryListItem.Revision
import org.wordpress.android.util.extensions.getParcelableCompat

class HistoryDetailActivity : LocaleAwareActivity() {
    companion object {
        const val KEY_HISTORY_DETAIL_FRAGMENT = "history_detail_fragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(HistoryDetailActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
            setSupportActionBar(toolbarMain)
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this) { AnalyticsTracker.track(Stat.REVISIONS_DETAIL_CANCELLED) }

        val extras = requireNotNull(intent.extras)
        val revision = extras.getParcelableCompat<Revision>(HistoryDetailContainerFragment.EXTRA_CURRENT_REVISION)
        val previousRevisionsIds =
            extras.getLongArray(HistoryDetailContainerFragment.EXTRA_PREVIOUS_REVISIONS_IDS)
        val postId = extras.getLong(HistoryDetailContainerFragment.EXTRA_POST_ID)
        val siteId = extras.getLong(HistoryDetailContainerFragment.EXTRA_SITE_ID)
        var historyDetailContainerFragment = supportFragmentManager.findFragmentByTag(KEY_HISTORY_DETAIL_FRAGMENT)

        if (historyDetailContainerFragment == null) {
            historyDetailContainerFragment =
                HistoryDetailContainerFragment.newInstance(revision, previousRevisionsIds, postId, siteId)
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
}
