package org.wordpress.android.ui.history

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.history.HistoryListItem.Revision
import javax.inject.Inject

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.history_activity)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val extras = intent.extras
        val revision = extras.getParcelable(HistoryDetailContainerFragment.EXTRA_REVISION) as Revision
        val revisions = extras.getParcelableArrayList<Revision>(HistoryDetailContainerFragment.EXTRA_REVISIONS)

        val fragment = HistoryDetailContainerFragment.newInstance(revision, revisions as ArrayList<Revision>)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

}