package org.wordpress.android.ui.engagement

import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.toolbar_main.*
import org.wordpress.android.R
import org.wordpress.android.ui.LocaleAwareActivity

class EngagedPeopleListActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.engaged_people_list_activity)

        val listScenario = intent.getParcelableExtra<ListScenario>(KEY_LIST_SCENARIO)

        setSupportActionBar(toolbar_main)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)

            it.title = if (listScenario.headerData.numLikes == 1) {
                getString(R.string.like_title_single_text)
            } else {
                getString(R.string.like_title_multiple_text, listScenario.headerData.numLikes)
            }
        }

        val fm = supportFragmentManager
        var likeListFragment = fm.findFragmentByTag(TAG_LIKE_LIST_FRAGMENT) as? EngagedPeopleListFragment

        if (likeListFragment == null) {
            likeListFragment = EngagedPeopleListFragment.newInstance(listScenario)
            fm.beginTransaction()
                    .add(R.id.fragment_container, likeListFragment, TAG_LIKE_LIST_FRAGMENT)
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val KEY_LIST_SCENARIO = "list_scenario"
        private const val TAG_LIKE_LIST_FRAGMENT = "tag_like_list_fragment"
    }
}
