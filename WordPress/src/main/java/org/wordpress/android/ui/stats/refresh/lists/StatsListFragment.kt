package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.stats.StatsConstants
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.StatsUtils
import org.wordpress.android.ui.stats.models.StatsPostModel
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.AddNewPost
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.SharePost
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewClicks
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewCommentsStats
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewFollowersStats
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPost
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPostDetailStats
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPostsAndPages
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPublicizeStats
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewReferrers
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewTag
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewTagsAndCategoriesStats
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewUrl
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewVideoPlays
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.DaysListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.MonthsListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.WeeksListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.YearsListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsListViewModel
import org.wordpress.android.util.Event
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.observeEvent
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

class StatsListFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    private lateinit var viewModel: StatsListViewModel

    private var linearLayoutManager: LinearLayoutManager? = null

    private val listStateKey = "list_state"

    companion object {
        private const val typeKey = "type_key"

        fun newInstance(section: StatsSection): StatsListFragment {
            val fragment = StatsListFragment()
            val bundle = Bundle()
            bundle.putSerializable(typeKey, section)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_list_fragment, container, false)
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
        recyclerView.addItemDecoration(
                RecyclerItemDecoration(
                        resources.getDimensionPixelSize(R.dimen.margin_medium),
                        resources.getDimensionPixelSize(R.dimen.margin_medium)
                )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity, savedInstanceState)
    }

    private fun initializeViewModels(activity: FragmentActivity, savedInstanceState: Bundle?) {
        val statsSection = arguments?.getSerializable(typeKey) as StatsSection

        val viewModelClass = when (statsSection) {
            StatsSection.INSIGHTS -> InsightsListViewModel::class.java
            StatsSection.DAYS -> DaysListViewModel::class.java
            StatsSection.WEEKS -> WeeksListViewModel::class.java
            StatsSection.MONTHS -> MonthsListViewModel::class.java
            StatsSection.YEARS -> YearsListViewModel::class.java
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(statsSection.name, viewModelClass)

        val site = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(activity.intent)
            nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        setupObservers(activity, site)
    }

    private fun setupObservers(activity: FragmentActivity, site: SiteModel) {
        viewModel.data.observe(this, Observer {
            if (it != null) {
                updateInsights(it)
            }
        })

        viewModel.navigationTarget.observeEvent(this) {
            when (it) {
                is AddNewPost -> ActivityLauncher.addNewPostForResult(activity, site, false)
                is ViewPost -> {
                    StatsUtils.openPostInReaderOrInAppWebview(
                            activity,
                            site.siteId,
                            it.postId.toString(),
                            it.postType,
                            it.postUrl
                    )
                }
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
                is ViewPostDetailStats -> {
                    val postModel = StatsPostModel(
                            site.siteId,
                            it.postId.toString(),
                            it.postTitle,
                            it.postUrl,
                            it.postType
                    )
                    ActivityLauncher.viewStatsSinglePostDetails(activity, postModel)
                }
                is ViewFollowersStats -> {
                    ActivityLauncher.viewFollowersStats(activity, site)
                }
                is ViewCommentsStats -> {
                    ActivityLauncher.viewCommentsStats(activity, site)
                }
                is ViewTagsAndCategoriesStats -> {
                    ActivityLauncher.viewTagsAndCategoriesStats(activity, site)
                }
                is ViewTag -> {
                    ActivityLauncher.openStatsUrl(activity, it.link)
                }
                is ViewPublicizeStats -> {
                    ActivityLauncher.viewPublicizeStats(activity, site)
                }
                is ViewPostsAndPages -> {
                    ActivityLauncher.viewPostsAndPagesStats(
                            activity,
                            site,
                            it.statsGranularity.toStatsTimeFrame(),
                            it.selectedDate
                    )
                }
                is ViewReferrers -> {
                    ActivityLauncher.viewReferrersStats(
                            activity,
                            site,
                            it.statsGranularity.toStatsTimeFrame(),
                            it.selectedDate
                    )
                }
                is ViewClicks -> {
                    ActivityLauncher.viewClicksStats(
                            activity,
                            site,
                            it.statsGranularity.toStatsTimeFrame(),
                            it.selectedDate
                    )
                }
                is ViewVideoPlays -> {
                    ActivityLauncher.viewVideoPlays(
                            activity,
                            site,
                            it.statsGranularity.toStatsTimeFrame(),
                            it.selectedDate
                    )
                }
                is ViewUrl -> {
                    WPWebViewActivity.openURL(activity, it.url)
                }
            }
            true
        }
    }

    private fun updateInsights(statsState: List<StatsBlock>) {
        val adapter: StatsBlockAdapter
        if (recyclerView.adapter == null) {
            adapter = StatsBlockAdapter(imageManager)
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as StatsBlockAdapter
        }
        val layoutManager = recyclerView?.layoutManager
        val recyclerViewState = layoutManager?.onSaveInstanceState()
        adapter.update(statsState)
        layoutManager?.onRestoreInstanceState(recyclerViewState)
    }
}

sealed class NavigationTarget : Event() {
    class AddNewPost : NavigationTarget()
    data class ViewPost(val postId: Long, val postUrl: String, val postType: String = StatsConstants.ITEM_TYPE_POST) :
            NavigationTarget()

    data class SharePost(val url: String, val title: String) : NavigationTarget()
    data class ViewPostDetailStats(
        val postId: Long,
        val postTitle: String,
        val postUrl: String,
        val postType: String = StatsConstants.ITEM_TYPE_POST
    ) : NavigationTarget()

    class ViewFollowersStats : NavigationTarget()
    class ViewCommentsStats : NavigationTarget()
    class ViewTagsAndCategoriesStats : NavigationTarget()
    class ViewPublicizeStats : NavigationTarget()
    data class ViewTag(val link: String) : NavigationTarget()
    data class ViewPostsAndPages(val statsGranularity: StatsGranularity, val selectedDate: String) : NavigationTarget()
    data class ViewReferrers(val statsGranularity: StatsGranularity, val selectedDate: String) : NavigationTarget()
    data class ViewClicks(val statsGranularity: StatsGranularity, val selectedDate: String) : NavigationTarget()
    data class ViewVideoPlays(val statsGranularity: StatsGranularity, val selectedDate: String) : NavigationTarget()
    data class ViewUrl(val url: String) : NavigationTarget()
}

fun StatsGranularity.toStatsTimeFrame(): StatsTimeframe {
    return when (this) {
        DAYS -> StatsTimeframe.DAY
        WEEKS -> StatsTimeframe.WEEK
        MONTHS -> StatsTimeframe.MONTH
        YEARS -> StatsTimeframe.YEAR
    }
}
