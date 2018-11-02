package org.wordpress.android.ui.history

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.EditPostActivity.EXTRA_POST_LOCAL_ID
import javax.inject.Inject

class HistoryActivity : AppCompatActivity() {
    @Inject lateinit var postStore: PostStore

    private  var mPost: PostModel? = null
    private  var mSite: SiteModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.history_activity)

        val extras = intent.extras
        mPost = postStore.getPostByLocalPostId(extras.getInt(EXTRA_POST_LOCAL_ID))
        mSite = intent.getSerializableExtra(WordPress.SITE) as SiteModel

        val fragment = HistoryListFragment.newInstance(mPost!!, mSite!!)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()

    }
}