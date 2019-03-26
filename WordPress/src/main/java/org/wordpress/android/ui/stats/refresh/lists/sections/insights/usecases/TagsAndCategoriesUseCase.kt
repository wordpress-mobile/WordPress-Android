package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.TagsModel
import org.wordpress.android.fluxc.model.stats.TagsModel.TagModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TAGS_AND_CATEGORIES
import org.wordpress.android.fluxc.store.stats.insights.TagsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewTag
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewTagsAndCategoriesStats
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TagsAndCategoriesUseCase.TagsAndCategoriesUiState
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

private const val BLOCK_ITEM_COUNT = 6
private const val VIEW_ALL_ITEM_COUNT = 1000

class TagsAndCategoriesUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val tagsStore: TagsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val resourceProvider: ResourceProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val useCaseMode: UseCaseMode
) : StatefulUseCase<TagsModel, TagsAndCategoriesUiState>(
        TAGS_AND_CATEGORIES,
        mainDispatcher,
        TagsAndCategoriesUiState(null)
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_ITEM_COUNT else BLOCK_ITEM_COUNT

    override suspend fun fetchRemoteData(forced: Boolean): State<TagsModel> {
        val response = tagsStore.fetchTags(statsSiteProvider.siteModel, LimitMode.Top(itemsToLoad), forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.tags.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override suspend fun loadCachedData(): TagsModel? {
        return tagsStore.getTags(statsSiteProvider.siteModel, LimitMode.Top(itemsToLoad))
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_tags_and_categories))

    override fun buildStatefulUiModel(domainModel: TagsModel, uiState: TagsAndCategoriesUiState): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == BLOCK) {
            items.add(Title(R.string.stats_insights_tags_and_categories))
        }

        if (domainModel.tags.isEmpty()) {
            items.add(Empty())
        } else {
            items.add(
                    Header(
                            string.stats_tags_and_categories_title_label,
                            string.stats_tags_and_categories_views_label
                    )
            )
            val tagsList = mutableListOf<BlockListItem>()
            domainModel.tags.forEachIndexed { index, tag ->
                when {
                    tag.items.size == 1 -> {
                        tagsList.add(mapTag(tag, index, domainModel.tags.size))
                    }
                    else -> {
                        val isExpanded = areTagsEqual(tag, uiState.expandedTag)
                        tagsList.add(ExpandableItem(
                                mapCategory(tag, index, domainModel.tags.size),
                                isExpanded
                        ) { changedExpandedState ->
                            onUiState(uiState.copy(expandedTag = if (changedExpandedState) tag else null))
                        })
                        if (isExpanded) {
                            tagsList.addAll(tag.items.map { subTag -> mapItem(subTag) })
                            tagsList.add(Divider)
                        }
                    }
                }
            }

            items.addAll(tagsList)
            if (useCaseMode == BLOCK && domainModel.hasMore) {
                items.add(
                        Link(
                                text = R.string.stats_insights_view_more,
                                navigateAction = NavigationAction.create(this::onLinkClick)
                        )
                )
            }
        }
        return items
    }

    private fun areTagsEqual(tagA: TagModel, tagB: TagModel?): Boolean {
        return tagA.items == tagB?.items && tagA.views == tagB.views
    }

    private fun mapTag(tag: TagsModel.TagModel, index: Int, listSize: Int): ListItemWithIcon {
        val item = tag.items.first()
        return ListItemWithIcon(
                icon = getIcon(item.type),
                text = item.name,
                value = tag.views.toFormattedString(),
                showDivider = index < listSize - 1,
                navigationAction = NavigationAction.create(item.link, this::onTagClick)
        )
    }

    private fun mapCategory(tag: TagsModel.TagModel, index: Int, listSize: Int): ListItemWithIcon {
        val text = tag.items.foldIndexed("") { itemIndex, acc, item ->
            when (itemIndex) {
                0 -> item.name
                else -> resourceProvider.getString(R.string.stats_category_folded_name, acc, item.name)
            }
        }
        return ListItemWithIcon(
                icon = R.drawable.ic_folder_multiple_white_24dp,
                text = text,
                value = tag.views.toFormattedString(),
                showDivider = index < listSize - 1
        )
    }

    private fun mapItem(item: TagModel.Item): ListItemWithIcon {
        return ListItemWithIcon(
                icon = getIcon(item.type),
                textStyle = LIGHT,
                text = item.name,
                showDivider = false,
                navigationAction = NavigationAction.create(item.link, this::onTagClick)
        )
    }

    private fun getIcon(type: String) =
            if (type == "tag") drawable.ic_tag_white_24dp else drawable.ic_folder_white_24dp

    private fun onLinkClick() {
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_TAGS_AND_CATEGORIES_VIEW_MORE_TAPPED)
        navigateTo(ViewTagsAndCategoriesStats())
    }

    private fun onTagClick(link: String) {
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_TAGS_AND_CATEGORIES_VIEW_TAG_TAPPED)
        navigateTo(ViewTag(link))
    }

    data class TagsAndCategoriesUiState(val expandedTag: TagModel? = null)

    class TagsAndCategoriesUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val tagsStore: TagsStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val resourceProvider: ResourceProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
                TagsAndCategoriesUseCase(
                        mainDispatcher,
                        tagsStore,
                        statsSiteProvider,
                        resourceProvider,
                        analyticsTracker,
                        useCaseMode
                )
    }
}
