package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.LayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_date_selector.*
import kotlinx.android.synthetic.main.stats_error_view.*
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
import org.wordpress.android.ui.stats.StatsConstants
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.StatsUtils
import org.wordpress.android.ui.stats.models.StatsPostModel
import org.wordpress.android.ui.stats.refresh.StatsListItemDecoration
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.AddNewPost
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.SharePost
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewAuthors
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewClicks
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewCommentsStats
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewCountries
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewFollowersStats
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPost
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPostDetailStats
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPostsAndPages
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPublicizeStats
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewReferrers
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewSearchTerms
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewTag
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewTagsAndCategoriesStats
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewUrl
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewVideoPlays
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.DaysListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.MonthsListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.WeeksListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.YearsListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsListViewModel
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.util.Event
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.observeEvent
import java.util.Date
import javax.inject.Inject

class StatsListFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var statsDateFormatter: StatsDateFormatter
    private lateinit var viewModel: StatsListViewModel

    private var layoutManager: LayoutManager? = null

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
        layoutManager?.let {
            outState.putParcelable(listStateKey, it.onSaveInstanceState())
        }

        val intent = activity?.intent
        if (intent != null && intent.hasExtra(WordPress.SITE)) {
            outState.putSerializable(WordPress.SITE, intent.getSerializableExtra(WordPress.SITE))
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
                        resources.getDimensionPixelSize(dimen.stats_list_card_horizontal_spacing),
                        resources.getDimensionPixelSize(dimen.stats_list_card_top_spacing),
                        resources.getDimensionPixelSize(dimen.stats_list_card_bottom_spacing),
                        resources.getDimensionPixelSize(dimen.stats_list_card_first_spacing),
                        resources.getDimensionPixelSize(dimen.stats_list_card_last_spacing),
                        columns
                )
        )
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollVertically(1) && dy != 0) {
                    viewModel.onScrolledToBottom()
                }
            }
        })
        select_next_date.setOnClickListener {
            viewModel.onNextDateSelected()
        }
        select_previous_date.setOnClickListener {
            viewModel.onPreviousDateSelected()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity)
    }

    private fun initializeViewModels(activity: FragmentActivity) {
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

        setupObservers(activity)
        viewModel.start()
    }

    private fun setupObservers(activity: FragmentActivity) {
        viewModel.uiModel.observe(this, Observer {
            if (it != null) {
                when (it) {
                    is UiModel.Success -> {
                        updateInsights(it.data)
                    }
                    is UiModel.Error -> {
                        recyclerView.visibility = View.GONE
                        actionable_error_view.visibility = View.VISIBLE
                        actionable_error_view.button.setOnClickListener {
                            viewModel.onRetryClick()
                        }
                    }
                }
            }
        })

        viewModel.showDateSelector.observe(this, Observer { dateSelectorUiModel ->
            val dateSelectorVisibility = if (dateSelectorUiModel?.isVisible == true) View.VISIBLE else View.GONE
            if (date_selection_toolbar.visibility != dateSelectorVisibility) {
                date_selection_toolbar.visibility = dateSelectorVisibility
            }
            selected_date.text = dateSelectorUiModel?.date ?: ""
            val enablePreviousButton = dateSelectorUiModel?.enableSelectPrevious == true
            if (select_previous_date.isEnabled != enablePreviousButton) {
                select_previous_date.isEnabled = enablePreviousButton
            }
            val enableNextButton = dateSelectorUiModel?.enableSelectNext == true
            if (select_next_date.isEnabled != enableNextButton) {
                select_next_date.isEnabled = enableNextButton
            }
        })

        viewModel.navigationTarget.observeEvent(this) {
            when (it) {
                is AddNewPost -> ActivityLauncher.addNewPostForResult(activity, it.site, false)
                is ViewPost -> {
                    StatsUtils.openPostInReaderOrInAppWebview(
                            activity,
                            it.siteId,
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
                            it.siteId,
                            it.postId,
                            it.postTitle,
                            it.postUrl,
                            it.postType
                    )
                    ActivityLauncher.viewStatsSinglePostDetails(activity, postModel)
                }
                is ViewFollowersStats -> {
                    ActivityLauncher.viewFollowersStats(activity, it.site)
                }
                is ViewCommentsStats -> {
                    ActivityLauncher.viewCommentsStats(activity, it.site)
                }
                is ViewTagsAndCategoriesStats -> {
                    ActivityLauncher.viewTagsAndCategoriesStats(activity, it.site)
                }
                is ViewTag -> {
                    ActivityLauncher.openStatsUrl(activity, it.link)
                }
                is ViewPublicizeStats -> {
                    ActivityLauncher.viewPublicizeStats(activity, it.site)
                }
                is ViewPostsAndPages -> {
                    ActivityLauncher.viewPostsAndPagesStats(
                            activity,
                            it.site,
                            it.statsGranularity.toStatsTimeFrame(),
                            statsDateFormatter.printStatsDate(it.selectedDate)
                    )
                }
                is ViewReferrers -> {
                    ActivityLauncher.viewReferrersStats(
                            activity,
                            it.site,
                            it.statsGranularity.toStatsTimeFrame(),
                            statsDateFormatter.printStatsDate(it.selectedDate)
                    )
                }
                is ViewClicks -> {
                    ActivityLauncher.viewClicksStats(
                            activity,
                            it.site,
                            it.statsGranularity.toStatsTimeFrame(),
                            statsDateFormatter.printStatsDate(it.selectedDate)
                    )
                }
                is ViewCountries -> {
                    ActivityLauncher.viewCountriesStats(
                            activity,
                            it.site,
                            it.statsGranularity.toStatsTimeFrame(),
                            statsDateFormatter.printStatsDate(it.selectedDate)
                    )
                }
                is ViewVideoPlays -> {
                    ActivityLauncher.viewVideoPlays(
                            activity,
                            it.site,
                            it.statsGranularity.toStatsTimeFrame(),
                            statsDateFormatter.printStatsDate(it.selectedDate)
                    )
                }
                is ViewSearchTerms -> {
                    ActivityLauncher.viewSearchTerms(
                            activity,
                            it.site,
                            it.statsGranularity.toStatsTimeFrame(),
                            statsDateFormatter.printStatsDate(it.selectedDate)
                    )
                }
                is ViewAuthors -> {
                    ActivityLauncher.viewAuthorsStats(
                            activity,
                            it.site,
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
        recyclerView.visibility = View.VISIBLE
        actionable_error_view.visibility = View.GONE
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
    data class AddNewPost(val site: SiteModel) : NavigationTarget()
    data class ViewPost(
        val postId: Long,
        val postUrl: String,
        val postType: String = StatsConstants.ITEM_TYPE_POST,
        val siteId: Long
    ) :
            NavigationTarget()

    data class SharePost(val url: String, val title: String) : NavigationTarget()
    data class ViewPostDetailStats(
        val postId: String,
        val postTitle: String,
        val postUrl: String?,
        val postType: String = StatsConstants.ITEM_TYPE_POST,
        val siteId: Long
    ) : NavigationTarget()

    data class ViewFollowersStats(val site: SiteModel) : NavigationTarget()
    data class ViewCommentsStats(val site: SiteModel) : NavigationTarget()
    data class ViewTagsAndCategoriesStats(val site: SiteModel) : NavigationTarget()
    data class ViewPublicizeStats(val site: SiteModel) : NavigationTarget()
    data class ViewTag(val link: String) : NavigationTarget()
    data class ViewPostsAndPages(val statsGranularity: StatsGranularity, val selectedDate: Date, val site: SiteModel) :
            NavigationTarget()

    data class ViewReferrers(val statsGranularity: StatsGranularity, val selectedDate: Date, val site: SiteModel) :
            NavigationTarget()

    data class ViewClicks(val statsGranularity: StatsGranularity, val selectedDate: Date, val site: SiteModel) :
            NavigationTarget()

    data class ViewCountries(val statsGranularity: StatsGranularity, val selectedDate: Date, val site: SiteModel) :
            NavigationTarget()

    data class ViewVideoPlays(val statsGranularity: StatsGranularity, val selectedDate: Date, val site: SiteModel) :
            NavigationTarget()

    data class ViewSearchTerms(val statsGranularity: StatsGranularity, val selectedDate: Date, val site: SiteModel) :
            NavigationTarget()

    data class ViewAuthors(val statsGranularity: StatsGranularity, val selectedDate: Date, val site: SiteModel) :
            NavigationTarget()

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
