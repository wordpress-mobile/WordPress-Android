package org.wordpress.android.ui.newstats.poststats

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.engagement.EngagedPeopleListActivity
import org.wordpress.android.ui.engagement.EngagementNavigationSource
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsColors
import org.wordpress.android.ui.newstats.selectNewStatsSiteIfNeeded
import org.wordpress.android.ui.newstats.analytics.NewStatsTracker
import org.wordpress.android.ui.stats.refresh.utils.StatsLaunchedFrom
import org.wordpress.android.ui.stats.refresh.utils.trackStatsAccessed
import org.wordpress.android.ui.newstats.components.StatsDayViewsChart
import org.wordpress.android.ui.newstats.components.StatsCardContainer
import org.wordpress.android.ui.newstats.datasource.PostViewsDailyView
import org.wordpress.android.ui.newstats.datasource.PostViewsData
import org.wordpress.android.ui.newstats.util.ProvideShimmerBrush
import org.wordpress.android.ui.newstats.util.ShimmerBox
import org.wordpress.android.ui.newstats.util.formatChangePercentage
import org.wordpress.android.ui.newstats.util.TEN_THOUSAND
import org.wordpress.android.ui.newstats.util.formatStatValue
import org.wordpress.android.ui.newstats.util.formatStatsDate
import org.wordpress.android.ui.newstats.util.formatStatsDateTime
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

private val ScreenPadding = 16.dp
private val CardPadding = 16.dp
/**
 * Tail room under the last card. There is none at the top: [StatsCardContainer] brings a margin of
 * its own, which is the whole gap between the app bar and the first card everywhere else in New
 * Stats, and adding to it here left this screen's header sitting lower than any of its siblings.
 */
private val CardListBottomPadding = 8.dp
private val ChartHeight = 120.dp
private val HeaderCornerRadius = 8.dp
private val HeaderVerticalPadding = 8.dp
private val HeaderMinHeight = 48.dp

private val OpenItemIconSize = 16.dp
private val LikerAvatarSize = 32.dp
/** How far each face sits under the one before it, which is what makes the strip read as a pile. */
private val LikerAvatarOverlap = 10.dp
/** The gap punched out of a face by the one in front of it, drawn in the card's own colour. */
private val LikerAvatarRing = 2.dp
private val ChevronSize = 20.dp
/** As many faces as fit beside the count on a narrow screen; the rest become the overflow disc. */
private const val LIKERS_STRIP_MAX = 5
private const val LOADING_SHIMMER_CARD_COUNT = 3
private const val LOADING_SHIMMER_ROW_COUNT = 3

@AndroidEntryPoint
class PostStatsDetailActivity : BaseAppCompatActivity() {
    @Inject
    lateinit var newStatsTracker: NewStatsTracker

    @Inject
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Inject
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Inject
    lateinit var activityNavigator: ActivityNavigator

    @Inject
    lateinit var siteStore: SiteStore

    private val viewModel: PostStatsDetailViewModel
        by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Before anything reads the selected site: the view model resolves it when it
        // fetches, and the tracking below attributes the open to it.
        selectSiteFromIntentIfNeeded()

        // Only on a fresh launch: a rotation recreates this activity from the same Intent, and
        // re-reporting there would inflate the count. Same guard as NewStatsActivity's.
        if (savedInstanceState == null) {
            trackStatsAccessedIfLaunchedFromOutside()
            newStatsTracker.trackPostDetailsScreenShown()
        }

        val postId = intent.getLongExtra(ARG_POST_ID, NO_POST_ID)
        val postTitle = intent
            .getStringExtra(ARG_POST_TITLE).orEmpty()
        // The view model survives recreation, so only the first creation kicks off the fetch --
        // otherwise every rotation or theme change refetches the post's whole view history.
        if (savedInstanceState == null) {
            viewModel.loadData(postId)
        }

        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiState
                    .collectAsState()
                PostStatsDetailScreen(
                    uiState = uiState,
                    postTitle = postTitle,
                    onBackPressed =
                        onBackPressedDispatcher
                            ::onBackPressed,
                    onRetry = { viewModel.loadData(postId) },
                    onPreviousDay = viewModel::selectPreviousDay,
                    onNextDay = viewModel::selectNextDay,
                    onOpenItem = { action ->
                        openItem(postId, action)
                    },
                    onShowLikes = { likeCount ->
                        showLikes(postId, likeCount)
                    },
                    onShowComments = { showComments(postId) }
                )
            }
        }
    }

    private fun selectSiteFromIntentIfNeeded() = selectNewStatsSiteIfNeeded(
        intent?.getIntExtra(WordPress.LOCAL_SITE_ID, 0) ?: 0,
        siteStore,
        selectedSiteRepository
    )

    /**
     * Reports the open under the name both Stats screens share, so the rollout cohorts stay
     * comparable on this entry point - see [trackStatsAccessed]. The old per-post screen reported
     * it from StatsDetailViewModel, which this screen replaces.
     *
     * Only for an entry point outside New Stats. One that went through [NewStatsActivity] already
     * reported the open there, and reporting again here would count one visit twice, so the
     * absence of the extra means "already reported" rather than "unknown source".
     */
    private fun trackStatsAccessedIfLaunchedFromOutside() {
        val launchedFrom = intent?.getStringExtra(ARG_LAUNCHED_FROM)
            ?.let { name -> StatsLaunchedFrom.entries.firstOrNull { it.name == name } }
        // No selected site means nothing to attribute the open to, so skip rather than report a
        // half-formed event. Same choice NewStatsActivity makes.
        val site = selectedSiteRepository.getSelectedSite()
        if (launchedFrom != null && site != null) {
            analyticsTracker.trackStatsAccessed(site, launchedFrom.value, isNewStats = true)
        }
    }

    /**
     * Opens the item these stats belong to: a post goes to the Reader, and everything else --
     * pages and attachments -- to its own URL, which is the only way to tell which image an
     * attachment row refers to (CMM-2580).
     *
     * Only the post case matches the old stats header. That header discarded the item's type on
     * its way to `NavigationTarget.ViewPost`, whose `postType` then defaulted to
     * `StatsConstants.ITEM_TYPE_POST`, so it sent pages to the Reader as well. Routing them to
     * their own URL instead is deliberate: it is what every other New Stats row does with a URL.
     */
    private fun openItem(postId: Long, action: PostHeaderAction.Open) {
        val site = selectedSiteRepository.getSelectedSite()
        analyticsTracker.track(
            Stat.STATS_DETAIL_POST_TAPPED,
            site
        )
        // The Reader is addressed by blog id, so without a selected site to read it from the
        // item's own URL is all there is to go on.
        if (action is PostHeaderAction.OpenInReader && site != null) {
            ReaderActivityLauncher.showReaderPostDetail(
                this,
                site.siteId,
                postId
            )
        } else {
            activityNavigator.openInCustomTab(this, action.url)
        }
    }

    /**
     * Opens the people who liked this item, in the likes list the Reader and notifications use.
     * It needs the site the stats are being read for, so there is nothing to open without one.
     */
    private fun showLikes(postId: Long, likeCount: Long) {
        val site = selectedSiteRepository.getSelectedSite()
            ?: return
        EngagedPeopleListActivity.startForPostLikes(
            context = this,
            siteId = site.siteId,
            postId = postId,
            numLikes = likeCount.toInt(),
            source = EngagementNavigationSource.LIKE_STATS_LIST
        )
    }

    /**
     * Opens the item's comments, in the threaded comments screen the Reader and notifications
     * use. That screen addresses a post by blog id, so like the Reader case of [openItem] there
     * is nothing to open without a selected site to read it from.
     */
    private fun showComments(postId: Long) {
        val site = selectedSiteRepository.getSelectedSite()
            ?: return
        ReaderActivityLauncher.showReaderComments(
            this,
            site.siteId,
            postId,
            ReaderTracker.SOURCE_STATS
        )
    }

    companion object {
        private const val ARG_POST_ID = "post_id"
        private const val ARG_POST_TITLE = "post_title"
        private const val ARG_LAUNCHED_FROM = "launched_from"

        /**
         * @param postId the post to show stats for; 0 is the site's home page
         * @param postTitle shown until the response arrives, and kept as the title for the home
         * page, whose stats carry no post metadata
         * @param localSiteId the site these stats are for, travelling as the same LOCAL_SITE_ID
         * extra New Stats already uses. Omit it from inside New Stats, where the site is already
         * the selected one; name it from anywhere that can be open for a different site.
         * @param launchedFrom the tap source to report STATS_ACCESSED with. Omit it from inside
         * New Stats, where [NewStatsActivity] already reported the open; name it from an entry
         * point that reaches this screen without passing through one that does. See
         * [trackStatsAccessedIfLaunchedFromOutside].
         */
        fun start(
            context: Context,
            postId: Long,
            postTitle: String,
            localSiteId: Int? = null,
            launchedFrom: StatsLaunchedFrom? = null
        ) {
            context.startActivity(
                Intent(
                    context,
                    PostStatsDetailActivity::class.java
                )
                    .putExtra(ARG_POST_ID, postId)
                    .putExtra(ARG_POST_TITLE, postTitle)
                    .apply { localSiteId?.let { putExtra(WordPress.LOCAL_SITE_ID, it) } }
                    // By name, not as Serializable, for the same reason NewStatsActivity's
                    // buildIntent does it: an Intent that may be filled in outside our process.
                    .apply { launchedFrom?.let { putExtra(ARG_LAUNCHED_FROM, it.name) } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
private fun PostStatsDetailScreen(
    uiState: PostStatsDetailUiState,
    postTitle: String,
    onBackPressed: () -> Unit,
    onRetry: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenItem: (action: PostHeaderAction.Open) -> Unit,
    onShowLikes: (likeCount: Long) -> Unit,
    onShowComments: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                // Material 3 makes a top app bar 64dp; every toolbar in the rest of the app is
                // the 56dp this dimen holds, and the extra 8dp is dead space under the title on
                // a screen whose first card starts right below it.
                expandedHeight = dimensionResource(R.dimen.toolbar_height),
                title = {
                    Text(
                        text = stringResource(
                            R.string.stats_post_detail_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons
                                .AutoMirrored
                                .Filled.ArrowBack,
                            contentDescription =
                                stringResource(
                                    R.string.back
                                )
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is PostStatsDetailUiState.Loading ->
                    LoadingContent()
                is PostStatsDetailUiState.Error ->
                    ErrorContent(uiState.message, onRetry)
                is PostStatsDetailUiState.Loaded ->
                    LoadedContent(
                        state = uiState,
                        fallbackTitle = postTitle,
                        onPreviousDay = onPreviousDay,
                        onNextDay = onNextDay,
                        onOpenItem = onOpenItem,
                        onShowLikes = onShowLikes,
                        onShowComments = onShowComments
                    )
            }
        }
    }
}

/**
 * Placeholder cards, so the screen keeps the same card rhythm it will have once the data lands
 * instead of reflowing from a flat list into cards.
 */
@Composable
private fun LoadingContent() {
    ProvideShimmerBrush {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = CardListBottomPadding)
        ) {
            repeat(LOADING_SHIMMER_CARD_COUNT) {
                StatsCardContainer {
                    Column(
                        modifier = Modifier.padding(CardPadding),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(LOADING_SHIMMER_ROW_COUNT) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ScreenPadding),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun LoadedContent(
    state: PostStatsDetailUiState.Loaded,
    fallbackTitle: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenItem: (action: PostHeaderAction.Open) -> Unit,
    onShowLikes: (likeCount: Long) -> Unit,
    onShowComments: () -> Unit
) {
    val data = state.data
    // One card per section, the way every other New Stats surface is built: the cards supply
    // their own margins, so the list adds nothing but the tail room under the last one.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(bottom = CardListBottomPadding)
    ) {
        item {
            StatsCardContainer {
                // The header's own click target carries HeaderVerticalPadding, so the card takes
                // that much off its top padding to leave the title the usual CardPadding inset.
                Column(
                    modifier = Modifier.padding(
                        start = CardPadding,
                        end = CardPadding,
                        top = CardPadding -
                            HeaderVerticalPadding,
                        bottom = CardPadding
                    )
                ) {
                    PostHeader(
                        data = data,
                        likerAvatarUrls =
                            state.likerAvatarUrls,
                        fallbackTitle = fallbackTitle,
                        onOpenItem = onOpenItem,
                        onShowLikes = data.post?.let { post ->
                            { onShowLikes(post.likeCount) }
                        },
                        onShowComments = onShowComments
                    )
                }
            }
        }

        val selectedDay = state.selectedDay
        if (selectedDay != null) {
            item {
                StatsCardContainer {
                    Column(
                        modifier = Modifier
                            .padding(CardPadding)
                    ) {
                        DaySelector(
                            day = selectedDay,
                            hasPreviousDay =
                                state.hasPreviousDay,
                            hasNextDay = state.hasNextDay,
                            onPreviousDay = onPreviousDay,
                            onNextDay = onNextDay
                        )
                        SelectedDayViews(
                            views = selectedDay.views,
                            previousViews =
                                state.previousDay?.views
                        )
                        DayViewsChart(
                            days = state.chartDays,
                            selectedIndex =
                                state.selectedChartIndex
                        )
                    }
                }
            }
        }

        val drawnWeeks = data.weeks.filter { it.days.isNotEmpty() }
        if (drawnWeeks.isNotEmpty()) {
            item {
                StatsCardContainer {
                    Column(
                        modifier = Modifier
                            .padding(CardPadding)
                    ) {
                        SectionTitle(
                            R.string.stats_detail_recent_weeks
                        )
                        WeeksCalendar(weeks = drawnWeeks)
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        // The grid shades each day against the busiest one, which says nothing
                        // about how much the weeks add up to -- so the total is spelled out.
                        DetailRow(
                            label = stringResource(
                                R.string.stats_post_detail_total
                            ),
                            value = formatStatValue(
                                drawnWeeks.sumOf { it.total },
                                TEN_THOUSAND
                            )
                        )
                    }
                }
            }
        }

        if (data.years.isNotEmpty()) {
            item {
                StatsCardContainer {
                    Column(
                        modifier = Modifier
                            .padding(CardPadding)
                    ) {
                        SectionTitle(
                            R.string.stats_detail_months_and_years
                        )
                        MonthsCalendar(
                            years = remember(data.years) {
                                data.years.asMonthlyTotals()
                            },
                            descriptionPluralResId =
                                R.plurals
                                    .stats_post_detail_period_views
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        DetailRow(
                            label = stringResource(
                                R.string.stats_post_detail_total
                            ),
                            value = formatStatValue(
                                data.years.sumOf { it.total },
                                TEN_THOUSAND
                            )
                        )
                    }
                }
            }
        }

        if (data.averages.isNotEmpty()) {
            item {
                StatsCardContainer {
                    Column(
                        modifier = Modifier
                            .padding(CardPadding)
                    ) {
                        SectionTitle(
                            R.string
                                .stats_detail_average_views_per_day
                        )
                        MonthsCalendar(
                            years = remember(data.averages) {
                                data.averages.asMonthlyAverages()
                            },
                            descriptionPluralResId =
                                R.plurals
                                    .stats_post_detail_period_average
                        )
                        data.overallAverage()?.let { overall ->
                            HorizontalDivider(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                            )
                            DetailRow(
                                label = stringResource(
                                    R.string
                                        .stats_post_detail_average
                                ),
                                value = formatStatValue(
                                    overall,
                                    TEN_THOUSAND
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The item's average views per day over its whole life, which the card writes under its grid of
 * monthly averages.
 *
 * The API reports an overall per year only, so an item that spans a single year -- most of them --
 * takes the server's own figure. Over several years there is none to take, so it is worked out the
 * way the server works it out: the views the item has had over the days it has existed.
 */
private fun PostViewsData.overallAverage(): Long? = when {
    averages.size == 1 -> averages.first().overall
    dailyViews.isEmpty() -> null
    else -> totalViews / dailyViews.size
}

/**
 * The title, date and engagement counts belong to the post. The home page has none of them, so it
 * falls back to the title the caller supplied and shows views alone.
 *
 * Laid out as iOS lays it out: the title and its published line, then the faces of the people who
 * liked it, then a rule, then the totals strip.
 */
@Composable
private fun PostHeader(
    data: PostViewsData,
    likerAvatarUrls: List<String>,
    fallbackTitle: String,
    onOpenItem: (action: PostHeaderAction.Open) -> Unit,
    onShowLikes: (() -> Unit)?,
    onShowComments: () -> Unit
) {
    val post = data.post
    // The action carries the URL it needs, so the click lambda cannot be built for a header with
    // nothing to open.
    val headerAction = postHeaderAction(post?.url, post?.type, post?.status)
    val onOpenClick: (() -> Unit)? =
        (headerAction as? PostHeaderAction.Open)?.let { action ->
            { onOpenItem(action) }
        }
    // The comments screen is a Reader screen, addressed by blog id, so it can only be reached for
    // the items the title itself opens there -- not for a page, an attachment or an unpublished
    // item, whose counts stay as plain figures.
    val onCommentsClick: (() -> Unit)? = onShowComments.takeIf {
        headerAction is PostHeaderAction.OpenInReader && (post?.commentCount ?: 0L) > 0L
    }
    val openLabel = stringResource(R.string.stats_open_link)
    Column {
        // Title and date read as one thing and open as one thing, the way the old stats
        // "Showing stats for:" header did. Rounding and padding give the ripple a shape of its
        // own, so a press reads as a press rather than as a full-bleed flash.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(HeaderCornerRadius)
                )
                .then(
                    if (onOpenClick != null) {
                        Modifier
                            .clickable(
                                onClickLabel = openLabel,
                                onClick = onOpenClick
                            )
                            .heightIn(min = HeaderMinHeight)
                    } else {
                        Modifier
                    }
                )
                .padding(
                    vertical = HeaderVerticalPadding
                )
                .semantics(mergeDescendants = true) {}
        ) {
            Text(
                text = post?.title
                    ?.takeIf { it.isNotBlank() }
                    ?: fallbackTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (onOpenClick != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (post != null) {
                Spacer(modifier = Modifier.height(4.dp))
                PublishedLine(
                    date = post.date,
                    linked = onOpenClick != null
                )
            }
        }
        if (post != null && post.likeCount > 0 &&
            onShowLikes != null
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            LikersRow(
                avatarUrls = likerAvatarUrls,
                likeCount = post.likeCount,
                onClick = onShowLikes
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            HeaderMetric(
                labelResId = R.string.stats_views,
                value = data.totalViews,
                modifier = Modifier.weight(1f)
            )
            if (post != null) {
                HeaderMetric(
                    labelResId = R.string.stats_likes,
                    value = post.likeCount,
                    // Nothing to open for an item nobody has liked.
                    onClick = onShowLikes
                        ?.takeIf { post.likeCount > 0 },
                    // The same words the likers row uses, since the two open the same screen.
                    onClickLabel = likesLabel(post.likeCount),
                    modifier = Modifier.weight(1f)
                )
                HeaderMetric(
                    labelResId = R.string.stats_comments,
                    value = post.commentCount,
                    onClick = onCommentsClick,
                    onClickLabel = stringResource(
                        R.string.reader_view_comments
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * "Published <date>", with the "opens elsewhere" arrow beside it when there is something to open.
 * The arrow sits here rather than after the title -- which can wrap to three lines -- so that the
 * affordance stays where the eye lands, next to a line of fixed length. The whole title-and-date
 * block is still the click target, so the arrow needs no description of its own.
 */
@Composable
private fun PublishedLine(date: String, linked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(
                R.string.stats_post_detail_published,
                formatStatsDateTime(date)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
        if (linked) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored
                    .Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(OpenItemIconSize),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * The faces of the people who liked the item, and the count, opening the full list.
 *
 * The row appears as soon as the stats response says there are likes; the faces fill in when the
 * separate likers call lands, and simply never arrive if it fails.
 */
@Composable
private fun LikersRow(
    avatarUrls: List<String>,
    likeCount: Long,
    onClick: () -> Unit
) {
    val label = likesLabel(likeCount)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HeaderCornerRadius))
            .clickable(
                onClickLabel = label,
                onClick = onClick
            )
            .heightIn(min = HeaderMinHeight)
            .padding(vertical = HeaderVerticalPadding)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (avatarUrls.isNotEmpty()) {
            LikersStrip(
                avatarUrls = avatarUrls,
                likeCount = likeCount
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.AutoMirrored
                .Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(ChevronSize),
            tint = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
    }
}

/**
 * The overlapping pile of faces, trailed by a disc counting whoever did not fit. The overflow is
 * measured against the like count from the stats response rather than against how many likers
 * were fetched, which is only ever the first page.
 */
@Composable
private fun LikersStrip(
    avatarUrls: List<String>,
    likeCount: Long
) {
    val shown = avatarUrls.take(LIKERS_STRIP_MAX)
    val overflow = likeCount - shown.size
    Row(
        horizontalArrangement =
            Arrangement.spacedBy(-LikerAvatarOverlap)
    ) {
        shown.forEach { avatarUrl ->
            LikerAvatar(avatarUrl)
        }
        if (overflow > 0) {
            LikersOverflow(overflow)
        }
    }
}

@Composable
private fun LikerAvatar(avatarUrl: String) {
    val sizePx = with(LocalDensity.current) {
        LikerAvatarSize.roundToPx()
    }
    // Gravatars are served at whatever size is asked for, and everything else goes through
    // Photon, so the request is sized to the disc rather than to whatever the API handed back.
    val url = remember(avatarUrl, sizePx) {
        WPAvatarUtils.rewriteAvatarUrl(avatarUrl, sizePx)
    }
    AsyncImage(
        model = url,
        // The row describes itself once; a list of names here would only be read out twice.
        contentDescription = null,
        modifier = Modifier
            .size(LikerAvatarSize)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = LikerAvatarRing,
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
    )
}

@Composable
private fun LikersOverflow(count: Long) {
    Box(
        modifier = Modifier
            .size(LikerAvatarSize)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = LikerAvatarRing,
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            // Abbreviated from a thousand rather than from the ten this screen uses elsewhere:
            // the disc is a 32dp circle that clips what will not fit, so "+1.2K" fits where
            // "+1,235" is cut off at both ends. The same trade as a heat cell's figure.
            text = stringResource(
                R.string.stats_post_detail_more_likers,
                formatStatValue(count)
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
    }
}

/**
 * One of the header's totals: a quiet label over a display-sized value, echoing the metrics strip
 * iOS puts at the foot of the same card.
 */
@Composable
private fun HeaderMetric(
    @StringRes labelResId: Int,
    value: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null
) {
    Column(
        modifier = modifier
            // The ripple takes the column's own bounds rather than a padded box, so the
            // metrics stay aligned whether or not any of them is tappable.
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(HeaderCornerRadius))
                        .clickable(
                            onClickLabel = onClickLabel,
                            onClick = onClick
                        )
                        .semantics(mergeDescendants = true) {}
                } else {
                    Modifier
                }
            )
    ) {
        Text(
            text = stringResource(labelResId),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formatStatValue(value, TEN_THOUSAND),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            // Tinted like the title above, and for the same reason: on this card a coloured
            // figure is one that goes somewhere. A count of zero has nothing to open, so it is
            // left as plain as the views beside it rather than tinted and inert.
            color = if (onClick != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/** Reuses the likes list screen's own title, so the row and the screen it opens read alike. */
@Composable
private fun likesLabel(likeCount: Long): String = if (likeCount == 1L) {
    stringResource(R.string.like_title_singular)
} else {
    stringResource(R.string.like_title_plural, likeCount)
}

/**
 * The selected day with arrows to step through the history, mirroring the old stats date bar.
 */
@Composable
private fun DaySelector(
    day: PostViewsDailyView,
    hasPreviousDay: Boolean,
    hasNextDay: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatStatsDate(day.day),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onPreviousDay,
            enabled = hasPreviousDay
        ) {
            Icon(
                imageVector = Icons.AutoMirrored
                    .Filled.KeyboardArrowLeft,
                contentDescription = stringResource(
                    R.string.stats_previous_day
                )
            )
        }
        IconButton(
            onClick = onNextDay,
            enabled = hasNextDay
        ) {
            Icon(
                imageVector = Icons.AutoMirrored
                    .Filled.KeyboardArrowRight,
                contentDescription = stringResource(
                    R.string.stats_next_day
                )
            )
        }
    }
}

/**
 * The selected day's views and how they compare with the day before.
 */
@Composable
private fun SelectedDayViews(
    views: Long,
    previousViews: Long?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = formatStatValue(views, TEN_THOUSAND),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.stats_views),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant,
            modifier = Modifier
                .padding(start = 8.dp, bottom = 2.dp)
                .weight(1f)
        )
        if (previousViews != null) {
            DayChangeLabel(
                views = views,
                previous = previousViews
            )
        }
    }
}

@Composable
private fun DayChangeLabel(views: Long, previous: Long) {
    val difference = views - previous
    val percentage = when {
        previous == views -> formatChangePercentage(0.0)
        previous == 0L -> INFINITY
        else -> formatChangePercentage(
            difference.toDouble() / previous
        )
    }
    val positive = difference >= 0
    Text(
        text = stringResource(
            if (positive) {
                R.string.stats_traffic_increase
            } else {
                R.string.stats_traffic_change
            },
            formatStatValue(difference, TEN_THOUSAND),
            percentage
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = if (positive) {
            StatsColors.ChangeBadgePositive
        } else {
            StatsColors.ChangeBadgeNegative
        }
    )
}

@Composable
private fun DayViewsChart(
    days: List<PostViewsDailyView>,
    selectedIndex: Int
) {
    // Derived once per window so the chart's remember() keys stay stable across recompositions,
    // and so no date parsing happens on Vico's measure/draw path.
    val values = remember(days) { days.map { it.views } }
    val startLabel = remember(days) {
        formatStatsDate(days.first().day)
    }
    val endLabel = remember(days) {
        formatStatsDate(days.last().day)
    }
    StatsDayViewsChart(
        values = values,
        selectedIndex = selectedIndex,
        height = ChartHeight,
        startLabel = startLabel,
        endLabel = endLabel,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SectionTitle(titleResId: Int) {
    Text(
        text = stringResource(titleResId),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailing()
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

private const val INFINITY = "∞"
