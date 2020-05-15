package org.wordpress.android.ui.reader

import android.os.Bundle
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType.MAIN_READER
import javax.inject.Inject

/**
 * This Activity was created during ReaderImprovements project. Extracting and refactoring the search from
 * ReaderPostListFragment was out-of-scope. This workaround enabled us writing new "discover" and "following" screens
 * into new tested classes without requiring us to change the search behavior.
 */
class ReaderSearchActivity : LocaleAwareActivity() {
    @Inject lateinit var readerTracker: ReaderTracker

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
        readerTracker.start(MAIN_READER)
    }
}
