package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.StatsStore.ManagementType
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BigTitle
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.DialogButtons
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ImageItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Tag
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.utils.NewsCardHandler
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

class ManagementNewsCardUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val resourceProvider: ResourceProvider,
    private val newsCardHandler: NewsCardHandler
) : StatelessUseCase<Boolean>(ManagementType.NEWS_CARD, mainDispatcher) {
    override suspend fun loadCachedData() = true

    override suspend fun fetchRemoteData(forced: Boolean): State<Boolean> = State.Data(true)

    override fun buildLoadingItem(): List<BlockListItem> = listOf()

    override fun buildUiModel(domainModel: Boolean): List<BlockListItem> {
        val editText = resourceProvider.getString(R.string.stats_management_edit)
        val newsCardMessage = resourceProvider.getString(R.string.stats_management_news_card_message, editText)
        return listOf(
                ImageItem(R.drawable.insights_management_feature_resource),
                Tag(R.string.stats_management_new),
                BigTitle(R.string.stats_manage_your_stats),
                Text(text = newsCardMessage, bolds = listOf(editText)),
                DialogButtons(
                        R.string.stats_management_try_it_now,
                        NavigationAction.create(this::onEditInsights),
                        R.string.stats_management_dismiss_insights_news_card,
                        NavigationAction.create(this::onDismiss)
                )
        )
    }

    private fun onEditInsights() {
        newsCardHandler.goToEdit()
    }

    private fun onDismiss() {
        newsCardHandler.dismiss()
    }
}
