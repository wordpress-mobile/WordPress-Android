package org.wordpress.android.ui.reader

import android.os.Bundle
import org.wordpress.android.R
import org.wordpress.android.ui.LocaleAwareActivity

/**
 * This Activity was created during ReaderImprovements project. Extracting and refactoring the search from
 * ReaderPostListFragment was out-of-scope. This workaround enabled us writing new "discover" and "following" screens
 * into new tested classes without requiring us to change the search behavior.
 */
class ReaderSearchActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reader_activity_search)

        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.add(R.id.fragment_container, ReaderPostListFragment.newInstanceForSearch())
            fragmentTransaction.commit()
        }
    }
}
