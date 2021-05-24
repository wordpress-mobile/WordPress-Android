package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel.Group
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.REFERRERS
import org.wordpress.android.fluxc.store.stats.time.ReferrersStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewReferrers
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewUrl
import org.wordpress.android.ui.stats.refresh.lists.BLOCK_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.VIEW_ALL_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.EMPTY_SPACE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.NORMAL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle.LIGHT
import org.wordpress.android.ui.utils.ListItemInteraction.Companion.create
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ReferrersUseCase.SelectedGroup
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.ReferrerPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.widgets.WPSnackbar
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class ReferrersUseCase(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val referrersStore: ReferrersStore,
    statsSiteProvider: StatsSiteProvider,
    selectedDateProvider: SelectedDateProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val contentDescriptionHelper: ContentDescriptionHelper,
    private val statsUtils: StatsUtils,
    private val useCaseMode: UseCaseMode,
    private val popupMenuHandler: ReferrerPopupMenuHandler
) : GranularStatefulUseCase<ReferrersModel, SelectedGroup>(
        REFERRERS,
        mainDispatcher,
        backgroundDispatcher,
        statsSiteProvider,
        selectedDateProvider,
        statsGranularity,
        SelectedGroup()
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_ITEM_COUNT else BLOCK_ITEM_COUNT

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_referrers))

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): ReferrersModel? {
        return referrersStore.getReferrers(
                site,
                statsGranularity,
                LimitMode.Top(itemsToLoad),
                selectedDate
        )
    }

    override suspend fun fetchRemoteData(selectedDate: Date, site: SiteModel, forced: Boolean): State<ReferrersModel> {
        val response = referrersStore.fetchReferrers(
                site,
                statsGranularity,
                LimitMode.Top(itemsToLoad),
                selectedDate,
                forced
        )
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.groups.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: ReferrersModel, uiState: SelectedGroup): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == BLOCK) {
            items.add(Title(R.string.stats_referrers))
        }

        if (domainModel.groups.isEmpty()) {
            items.add(Empty(R.string.stats_no_data_for_period))
        } else {
            val header = Header(R.string.stats_referrer_label, R.string.stats_referrer_views_label)
            items.add(header)
            domainModel.groups.forEachIndexed { index, group ->
                val contentDescription =
                        contentDescriptionHelper.buildContentDescription(
                                header,
                                group.name ?: "",
                                group.total ?: 0
                        )
                val spam = group.markedAsSpam
                val icon = buildIcon(group.icon, spam)
                if (group.referrers.isEmpty()) {
                    val headerItem = ListItemWithIcon(
                            icon = icon,
                            iconUrl = if (icon == null) group.icon else null,
                            textStyle = buildTextStyle(spam),
                            text = group.name,
                            value = group.total?.let { statsUtils.toFormattedString(it) },
                            showDivider = index < domainModel.groups.size - 1,
                            navigationAction = group.url?.let {
                                create(it, this::onItemClick)
                            },
                            longClickAction = { view -> this.onMenuClick(view, group, spam) },
                            contentDescription = contentDescription
                    )
                    items.add(headerItem)
                } else {
                    val headerItem = ListItemWithIcon(
                            icon = icon,
                            iconUrl = if (icon == null) group.icon else null,
                            textStyle = buildTextStyle(spam),
                            text = group.name,
                            value = group.total?.let { statsUtils.toFormattedString(it) },
                            showDivider = index < domainModel.groups.size - 1,
                            contentDescription = contentDescription,
                            longClickAction = { view -> this.onMenuClick(view, group, spam) }
                    )
                    val isExpanded = group.groupId == uiState.groupId
                    items.add(ExpandableItem(headerItem, isExpanded) { changedExpandedState ->
                        onUiState(SelectedGroup(if (changedExpandedState) group.groupId else null))
                    })
                    if (isExpanded) {
                        items.addAll(group.referrers.map { referrer ->
                            val referrerSpam = referrer.markedAsSpam
                            val referrerIcon = buildIcon(referrer.icon, referrerSpam)
                            val iconStyle = if (group.icon != null && referrer.icon == null && referrerIcon == null) {
                                EMPTY_SPACE
                            } else {
                                NORMAL
                            }
                            ListItemWithIcon(
                                    icon = referrerIcon,
                                    iconUrl = if (referrerIcon == null) referrer.icon else null,
                                    iconStyle = iconStyle,
                                    textStyle = buildTextStyle(referrerSpam),
                                    text = referrer.name,
                                    value = statsUtils.toFormattedString(referrer.views),
                                    showDivider = false,
                                    navigationAction = referrer.url?.let {
                                        create(it, this::onItemClick)
                                    },
                                    contentDescription = contentDescriptionHelper.buildContentDescription(
                                            header,
                                            referrer.name,
                                            referrer.views
                                    )
                            )
                        })
                        items.add(Divider)
                    }
                }
            }

            if (useCaseMode == BLOCK && domainModel.hasMore) {
                items.add(
                        Link(
                                text = R.string.stats_insights_view_more,
                                navigateAction = create(statsGranularity, this::onViewMoreClicked)
                        )
                )
            }
        }
        return items
    }

    private fun buildTextStyle(spam: Boolean) = if (spam) LIGHT else TextStyle.NORMAL

    private fun buildIcon(iconUrl: String?, spam: Boolean): Int? {
        return when {
            spam -> R.drawable.ic_spam_white_24dp
            iconUrl == null -> R.drawable.ic_globe_white_24dp
            iconUrl == "https://wordpress.com/i/stats/search-engine.png" -> R.drawable.ic_search_white_24dp
            else -> null
        }
    }

    private fun onViewMoreClicked(statsGranularity: StatsGranularity) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_REFERRERS_VIEW_MORE_TAPPED, statsGranularity)
        navigateTo(
                ViewReferrers(
                        statsGranularity,
                        selectedDateProvider.getSelectedDate(statsGranularity) ?: Date()
                )
        )
    }

    private fun onItemClick(url: String) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_REFERRERS_ITEM_TAPPED, statsGranularity)
        openWebsite(url)
    }

    fun openWebsite(url: String) {
        navigateTo(ViewUrl(url))
    }

    private fun onMenuClick(view: View, group: Group, spam: Boolean?): Boolean {
        val url = when {
            group.url != null -> group.url
            UrlUtils.isValidUrlAndHostNotNull("https://${group.name}") -> group.name
            else -> null
        }
        if (url != null) {
            analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_REFERRERS_ITEM_LONG_PRESSED, statsGranularity)
            popupMenuHandler.onMenuClick(view, statsGranularity, url, spam, this)
        } else {
            // Show snackbar with error message
            WPSnackbar.make(
                    view,
                    R.string.stats_referrer_snackbar_cant_mark_as_spam,
                    Snackbar.LENGTH_LONG
            ).show()
        }

        return true
    }

    suspend fun markReferrerAsSpam(urlDomain: String) {
        selectedDateProvider.getSelectedDate(statsGranularity)?.let {
            referrersStore.reportReferrerAsSpam(
                    statsSiteProvider.siteModel,
                    urlDomain,
                    statsGranularity,
                    LimitMode.Top(itemsToLoad),
                    it
            )
        }
    }

    suspend fun unmarkReferrerAsSpam(urlDomain: String) {
        selectedDateProvider.getSelectedDate(statsGranularity)?.let {
            referrersStore.unreportReferrerAsSpam(
                    statsSiteProvider.siteModel,
                    urlDomain,
                    statsGranularity,
                    LimitMode.Top(itemsToLoad),
                    it
            )
        }
    }

    data class SelectedGroup(val groupId: String? = null)

    class ReferrersUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val referrersStore: ReferrersStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val selectedDateProvider: SelectedDateProvider,
        private val contentDescriptionHelper: ContentDescriptionHelper,
        private val statsUtils: StatsUtils,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val popupMenuHandler: ReferrerPopupMenuHandler
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
                ReferrersUseCase(
                        granularity,
                        mainDispatcher,
                        backgroundDispatcher,
                        referrersStore,
                        statsSiteProvider,
                        selectedDateProvider,
                        analyticsTracker,
                        contentDescriptionHelper,
                        statsUtils,
                        useCaseMode,
                        popupMenuHandler
                )
    }
}
