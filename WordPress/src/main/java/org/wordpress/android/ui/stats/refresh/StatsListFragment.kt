package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.stats_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.stats.StatsConstants
import org.wordpress.android.ui.stats.models.StatsPostModel
import org.wordpress.android.ui.stats.refresh.NavigationTarget.AddNewPost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.SharePost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewMore
import org.wordpress.android.ui.stats.refresh.StatsListViewModel.StatsListType
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

class StatsListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: StatsListViewModel

    private var linearLayoutManager: LinearLayoutManager? = null

    private val listStateKey = "list_state"

    companion object {
        private const val typeKey = "type_key"

        fun newInstance(listType: StatsListType): StatsListFragment {
            val fragment = StatsListFragment()
            val bundle = Bundle()
            bundle.putSerializable(typeKey, listType)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        linearLayoutManager?.let {
            outState.putParcelable(listStateKey, it.onSaveInstanceState())
        }
        val intent = activity?.intent
        if (intent != null && intent.hasExtra(WordPress.SITE)) {
            outState.putSerializable(WordPress.SITE, intent.getSerializableExtra(WordPress.SITE))
        }
        super.onSaveInstanceState(outState)
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.addItemDecoration(RecyclerItemDecoration(0, DisplayUtils.dpToPx(activity, 5)))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)
        (nonNullActivity.application as? WordPress)?.component()?.inject(this)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity, savedInstanceState)
    }

    private fun initializeViewModels(activity: FragmentActivity, savedInstanceState: Bundle?) {
        val statsType = arguments?.getSerializable(typeKey) as StatsListType
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(statsType.name, StatsListViewModel::class.java)

        val site = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(activity.intent)
            nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }
        viewModel.start(site, statsType)

        setupObservers(site)
    }

    private fun setupObservers(site: SiteModel) {
        viewModel.data.observe(this, Observer {
            if (it != null) {
                updateInsights(it)
            }
        })

        viewModel.navigationTarget.observe(this, Observer {
            when (it) {
                is AddNewPost -> ActivityLauncher.addNewPostOrPageForResult(activity, site, false, false)
                is SharePost -> {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, it.url)
                    intent.putExtra(Intent.EXTRA_SUBJECT, it.title)
                    try {
                        startActivity(Intent.createChooser(intent, getString(R.string.share_link)))
                    } catch (ex: android.content.ActivityNotFoundException) {
                        ToastUtils.showToast(activity, R.string.reader_toast_err_share_intent)
                    }
                }
                is ViewMore -> {
                    val postModel = StatsPostModel(
                            it.siteID,
                            it.postID,
                            it.postTitle,
                            it.postUrl,
                            StatsConstants.ITEM_TYPE_POST
                    )
                    ActivityLauncher.viewStatsSinglePostDetails(activity, postModel)
                }
            }
        })
    }

    private fun updateInsights(insightsState: InsightsUiState) {
        val adapter: InsightsAdapter
        if (recyclerView.adapter == null) {
            adapter = InsightsAdapter()
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as InsightsAdapter
        }
        adapter.update(insightsState.data)
    }
}

sealed class NavigationTarget {
    object AddNewPost : NavigationTarget()
    data class SharePost(val url: String, val title: String) : NavigationTarget()
    data class ViewMore(val siteID: Long, val postID: String, val postTitle: String, val postUrl: String) :
            NavigationTarget()
}
