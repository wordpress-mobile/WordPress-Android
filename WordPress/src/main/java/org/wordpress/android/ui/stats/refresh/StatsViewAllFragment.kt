package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView.LayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.stats.StatsAbstractFragment
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.StatsUtils
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.models.StatsPostModel
import org.wordpress.android.ui.stats.refresh.NavigationTarget.AddNewPost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.SharePost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewAuthors
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewClicks
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewCommentsStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewCountries
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewFollowersStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPostDetailStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPostsAndPages
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPublicizeStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewReferrers
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewSearchTerms
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewTag
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewTagsAndCategoriesStats
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewUrl
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewVideoPlays
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlockAdapter
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.observeEvent
import javax.inject.Inject

class StatsViewAllFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var statsDateFormatter: StatsDateFormatter
    private lateinit var viewModel: StatsListViewModel

    private var layoutManager: LayoutManager? = null

    private val listStateKey = "list_state"

    companion object {
        private const val typeKey = "type_key"

        fun newInstance(statsType: StatsViewType): StatsViewAllFragment {
            val fragment = StatsViewAllFragment()
            val bundle = Bundle()
            bundle.putSerializable(typeKey, statsType)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_list_fragment, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        layoutManager?.let {
            outState.putParcelable(listStateKey, it.onSaveInstanceState())
        }

        val intent = activity?.intent
        if (intent != null) {
            if (intent.hasExtra(WordPress.SITE)) {
                outState.putSerializable(WordPress.SITE, intent.getSerializableExtra(WordPress.SITE))
            }
            if (intent.hasExtra(WordPress.SITE)) {
                outState.putSerializable(StatsAbstractFragment.ARGS_VIEW_TYPE,
                        intent.getSerializableExtra(StatsAbstractFragment.ARGS_VIEW_TYPE))
            }
        }

        super.onSaveInstanceState(outState)
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val columns = resources.getInteger(R.integer.stats_number_of_columns)
        val layoutManager: LayoutManager = if (columns == 1) {
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        } else {
            StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
        }
        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        this.layoutManager = layoutManager
        recyclerView.layoutManager = this.layoutManager
        recyclerView.addItemDecoration(
                StatsListItemDecoration(
                        resources.getDimensionPixelSize(dimen.margin_small),
                        resources.getDimensionPixelSize(dimen.margin_small),
                        columns
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
        val site = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(activity.intent)
            nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        val type = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(activity.intent)
            nonNullIntent.getSerializableExtra(StatsAbstractFragment.ARGS_VIEW_TYPE) as StatsViewType
        } else {
            savedInstanceState.getSerializable(StatsAbstractFragment.ARGS_VIEW_TYPE) as StatsViewType
        }

        val clazz = when (type) {
            StatsViewType.FOLLOWERS -> StatsViewAllFollowersViewModel::class.java
            else -> StatsViewAllFollowersViewModel::class.java
        }

        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(clazz)

        setupObservers(activity, site)

        (viewModel as StatsViewAllFollowersViewModel).start(site)
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
                            statsDateFormatter.printStatsDate(it.selectedDate)
                    )
                }
                is ViewReferrers -> {
                    ActivityLauncher.viewReferrersStats(
                            activity,
                            site,
                            it.statsGranularity.toStatsTimeFrame(),
                            statsDateFormatter.printStatsDate(it.selectedDate)
                    )
                }
                is ViewClicks -> {
                    ActivityLauncher.viewClicksStats(
                            activity,
                            site,
                            it.statsGranularity.toStatsTimeFrame(),
                            statsDateFormatter.printStatsDate(it.selectedDate)
                    )
                }
                is ViewCountries -> {
                    ActivityLauncher.viewCountriesStats(
                            activity,
                            site,
                            it.statsGranularity.toStatsTimeFrame(),
                            statsDateFormatter.printStatsDate(it.selectedDate)
                    )
                }
                is ViewVideoPlays -> {
                    ActivityLauncher.viewVideoPlays(
                            activity,
                            site,
                            it.statsGranularity.toStatsTimeFrame(),
                            statsDateFormatter.printStatsDate(it.selectedDate)
                    )
                }
                is ViewSearchTerms -> {
                    ActivityLauncher.viewSearchTerms(
                            activity,
                            site,
                            it.statsGranularity.toStatsTimeFrame(),
                            statsDateFormatter.printStatsDate(it.selectedDate)
                    )
                }
                is ViewAuthors -> {
                    ActivityLauncher.viewAuthorsStats(
                            activity,
                            site,
                            it.statsGranularity.toStatsTimeFrame(),
                            statsDateFormatter.printStatsDate(it.selectedDate)
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

fun StatsGranularity.toStatsTimeFrame(): StatsTimeframe {
    return when (this) {
        DAYS -> StatsTimeframe.DAY
        WEEKS -> StatsTimeframe.WEEK
        MONTHS -> StatsTimeframe.MONTH
        YEARS -> StatsTimeframe.YEAR
    }
}
