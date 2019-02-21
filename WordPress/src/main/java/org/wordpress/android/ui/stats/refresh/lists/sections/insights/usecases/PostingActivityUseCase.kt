package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.POSTING_ACTIVITY
import org.wordpress.android.fluxc.store.stats.insights.PostingActivityStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class PostingActivityUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val store: PostingActivityStore
) : StatelessUseCase<PostingActivityModel>(POSTING_ACTIVITY, mainDispatcher) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_all_time_stats))

    override suspend fun loadCachedData(site: SiteModel): PostingActivityModel? {
        return store.getPostingActivity(site, getStartDate(), getEndDate())
    }

    private fun getEndDate(): Date {
        val endDate = Calendar.getInstance()
        endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH))
        return endDate.time
    }

    private fun getStartDate(): Date {
        val startDate = Calendar.getInstance()
        startDate.add(Calendar.MONTH, -3)
        startDate.set(Calendar.DAY_OF_MONTH, startDate.getActualMinimum(Calendar.DAY_OF_MONTH))
        return startDate.time
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean): State<PostingActivityModel> {
        val response = store.fetchPostingActivity(site, getStartDate(), getEndDate(), forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.events.isNotEmpty() -> State.Data(
                    model
            )
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: PostingActivityModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_posting_activity))
        domainModel.events.forEach {
            items.add(ListItemWithIcon(text = it.date.toString(), value = it.postCount.toString()))
        }
        return items
    }
}
