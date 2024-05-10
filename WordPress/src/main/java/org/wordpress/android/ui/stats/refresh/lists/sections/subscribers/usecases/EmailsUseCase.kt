package org.wordpress.android.ui.stats.refresh.lists.sections.subscribers.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.subscribers.PostsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.EmailsRestClient.SortField
import org.wordpress.android.fluxc.store.StatsStore.SubscriberType.EMAILS
import org.wordpress.android.fluxc.store.stats.subscribers.EmailsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.BLOCK_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class EmailsUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val emailsStore: EmailsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsUtils: StatsUtils,
    private val contentDescriptionHelper: ContentDescriptionHelper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val useCaseMode: UseCaseMode
) : StatelessUseCase<PostsModel>(EMAILS, mainDispatcher, bgDispatcher) {
    private val itemsToShow = if (useCaseMode == VIEW_ALL) VIEW_ALL_ITEM_SIZE else BLOCK_ITEM_COUNT
    private val sortField = if (useCaseMode == VIEW_ALL) SortField.OPENS else SortField.POST_ID

    override suspend fun fetchRemoteData(forced: Boolean): State<PostsModel> {
        val response = emailsStore.fetchEmails(
            statsSiteProvider.siteModel,
            LimitMode.Top(VIEW_ALL_ITEM_SIZE),
            sortField,
            forced
        )
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.posts.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override suspend fun loadCachedData() =
        emailsStore.getEmails(statsSiteProvider.siteModel, LimitMode.Top(VIEW_ALL_ITEM_SIZE), sortField)

    override fun buildLoadingItem() = listOf(BlockListItem.Title(R.string.stats_view_emails))

    override fun buildEmptyItem() = listOf(buildTitle(), BlockListItem.Empty())

    override fun buildUiModel(domainModel: PostsModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == UseCaseMode.BLOCK) {
            items.add(buildTitle())
        }

        if (domainModel.posts.isEmpty()) {
            items.add(BlockListItem.Empty())
        } else {
            val header = BlockListItem.ListHeader(
                R.string.stats_emails_latest_emails_label,
                R.string.stats_emails_opens_label,
                R.string.stats_emails_clicks_label
            )
            items.add(header)
            val postsList = mutableListOf<BlockListItem>()
            domainModel.posts.take(itemsToShow).forEach { post ->
                val value1 = statsUtils.toFormattedString(post.opens)
                val value2 = statsUtils.toFormattedString(post.clicks)
                val listItem = BlockListItem.ListItemWithTwoValues(
                    text = post.title,
                    value1 = value1,
                    value2 = value2,
                    contentDescription = contentDescriptionHelper.buildContentDescription(
                        header,
                        post.title,
                        value1,
                        value2
                    )
                )
                postsList.add(listItem)
            }

            items.addAll(postsList)
            if (useCaseMode == UseCaseMode.BLOCK && domainModel.posts.size > BLOCK_ITEM_COUNT) {
                items.add(
                    BlockListItem.Link(
                        text = R.string.stats_insights_view_more,
                        navigateAction = ListItemInteraction.create(this::onLinkClick)
                    )
                )
            }
        }
        return items
    }

    private fun buildTitle() = BlockListItem.Title(R.string.stats_view_emails)

    private fun onLinkClick() {
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_EMAILS_VIEW_MORE_TAPPED)
        navigateTo(NavigationTarget.EmailsStats)
    }

    class EmailsUseCaseFactory @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val emailsStore: EmailsStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val statsUtils: StatsUtils,
        private val contentDescriptionHelper: ContentDescriptionHelper,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) = EmailsUseCase(
            mainDispatcher,
            backgroundDispatcher,
            emailsStore,
            statsSiteProvider,
            statsUtils,
            contentDescriptionHelper,
            analyticsTracker,
            useCaseMode
        )
    }

    companion object {
        private const val VIEW_ALL_ITEM_SIZE = 30
    }
}
