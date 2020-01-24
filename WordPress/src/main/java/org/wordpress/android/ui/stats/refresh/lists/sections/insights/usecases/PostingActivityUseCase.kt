package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import android.view.View
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel.Day
import org.wordpress.android.fluxc.store.StatsStore.InsightType.POSTING_ACTIVITY
import org.wordpress.android.fluxc.store.stats.insights.PostingActivityStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

class PostingActivityUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val store: PostingActivityStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val postingActivityMapper: PostingActivityMapper,
    private val popupMenuHandler: ItemPopupMenuHandler
) : StatelessUseCase<PostingActivityModel>(POSTING_ACTIVITY, mainDispatcher, backgroundDispatcher) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_posting_activity))

    override fun buildEmptyItem(): List<BlockListItem> {
        return listOf(buildTitle(), Empty())
    }

    override suspend fun loadCachedData(): PostingActivityModel? {
        return store.getPostingActivity(statsSiteProvider.siteModel, getStartDate(), getEndDate())
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<PostingActivityModel> {
        val response = store.fetchPostingActivity(statsSiteProvider.siteModel, getStartDate(), getEndDate(), forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.months.isNotEmpty() -> State.Data(
                    model
            )
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: PostingActivityModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(buildTitle())
        val activityItem = postingActivityMapper.buildActivityItem(
                domainModel.months,
                domainModel.max
        )
        items.add(activityItem)
        return items
    }

    private fun buildTitle() = Title(R.string.stats_insights_posting_activity, menuAction = this::onMenuClick)

    private fun getEndDate(): Day {
        val endDate = Calendar.getInstance()
        return Day(
                endDate.get(Calendar.YEAR),
                endDate.get(Calendar.MONTH),
                endDate.getActualMaximum(Calendar.DAY_OF_MONTH)
        )
    }

    private fun getStartDate(): Day {
        val startDate = Calendar.getInstance()
        startDate.add(Calendar.MONTH, -2)
        return Day(
                startDate.get(Calendar.YEAR),
                startDate.get(Calendar.MONTH),
                startDate.getActualMinimum(Calendar.DAY_OF_MONTH)
        )
    }

    private fun onMenuClick(view: View) {
        popupMenuHandler.onMenuClick(view, type)
    }
}
