package org.wordpress.android.ui.stats.refresh.lists.detail

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.toolbar.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListFragment
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import javax.inject.Inject

private const val POST_ID = "POST_ID"
private const val POST_TYPE = "POST_ID"
private const val POST_TITLE = "POST_TITLE"
private const val POST_URL = "POST_URL"

class StatsDetailActivity : AppCompatActivity() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        setContentView(R.layout.stats_detail_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        val site = intent?.getSerializableExtra(WordPress.SITE) as SiteModel?
        val postId = intent?.getSerializableExtra(POST_ID) as String?
        val postType = intent?.getSerializableExtra(POST_TYPE) as String?
        val postTitle = intent?.getSerializableExtra(POST_TITLE) as String?
        val postUrl = intent?.getSerializableExtra(POST_URL) as String?

        val viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(StatsSection.DETAIL.name, DetailListViewModel::class.java)
        viewModel.init(
                checkNotNull(site),
                checkNotNull(postId),
                checkNotNull(postType),
                checkNotNull(postTitle),
                postUrl
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun start(
            context: Context,
            site: SiteModel,
            postId: String,
            postType: String,
            postTitle: String,
            postUrl: String?
        ) {
            val statsPostViewIntent = Intent(context, StatsDetailActivity::class.java)
            statsPostViewIntent.putExtra(WordPress.SITE, site)
            statsPostViewIntent.putExtra(POST_ID, postId)
            statsPostViewIntent.putExtra(POST_TYPE, postType)
            statsPostViewIntent.putExtra(POST_TITLE, postTitle)
            statsPostViewIntent.putExtra(StatsListFragment.LIST_TYPE, StatsSection.DETAIL)
            if (postUrl != null) {
                statsPostViewIntent.putExtra(POST_URL, postUrl)
            }
            context.startActivity(statsPostViewIntent)
        }
    }
}
