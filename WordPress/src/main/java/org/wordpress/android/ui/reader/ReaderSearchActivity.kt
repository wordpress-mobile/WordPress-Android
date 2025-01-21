package org.wordpress.android.ui.reader

import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType.MAIN_READER
import org.wordpress.android.util.JetpackBrandingUtils
import javax.inject.Inject

/**
 * This Activity was created during ReaderImprovements project. Extracting and refactoring the search from
 * ReaderPostListFragment was out-of-scope. This workaround enabled us writing new "discover" and "following" screens
 * into new tested classes without requiring us to change the search behavior.
 */
@AndroidEntryPoint
class ReaderSearchActivity : BaseAppCompatActivity() {
    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.reader_activity_search)

        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.add(R.id.fragment_container, ReaderPostListFragment.newInstanceForSearch())
            fragmentTransaction.commit()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reader search used to be part of the MAIN_READER - it's still being tracked as MAIN_READER so we don't
        // introduce inconsistencies into the existing tracking data
        readerTracker.start(MAIN_READER)
    }

    override fun onPause() {
        super.onPause()
        readerTracker.stop(MAIN_READER)
    }

    fun finishWithRefreshSubscriptionsResult() {
        val data = Intent()
        data.putExtra(ReaderSubsActivity.RESULT_SHOULD_REFRESH_SUBSCRIPTIONS, true)
        setResult(RESULT_OK, data)
        finish()
    }

    companion object {
        const val RESULT_SHOULD_REFRESH_SUBSCRIPTIONS = "should_refresh_subscriptions"
    }
}
