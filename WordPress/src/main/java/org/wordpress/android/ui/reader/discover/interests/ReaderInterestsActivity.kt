package org.wordpress.android.ui.reader.discover.interests

import android.os.Bundle
import androidx.fragment.app.commit
import org.wordpress.android.ui.main.BaseAppCompatActivity

/**
 * Hosts the tags picker opened from Settings; the screen draws its own top bar
 */
class ReaderInterestsActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(android.R.id.content, ReaderInterestsFragment())
            }
        }
    }
}
