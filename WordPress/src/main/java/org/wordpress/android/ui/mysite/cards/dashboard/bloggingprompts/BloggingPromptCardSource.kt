package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.BloggingPromptUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

const val REFRESH_DELAY = 500L

class BloggingPromptCardSource @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val promptsStore: BloggingPromptsStore,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : MySiteRefreshSource<BloggingPromptUpdate> {
    override val refresh = MutableLiveData(false)

    override fun build(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<BloggingPromptUpdate> {
        val result = MediatorLiveData<BloggingPromptUpdate>()
        result.getData(coroutineScope, siteLocalId)
        result.addSource(refresh) {
            result.refreshData(coroutineScope, siteLocalId, refresh.value)
        }
        refresh()
        return result
    }

    private fun MediatorLiveData<BloggingPromptUpdate>.getData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ) {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            coroutineScope.launch(bgDispatcher) {
                promptsStore.getPromptForDate(selectedSite, Date()).collect { result ->
                    postValue(BloggingPromptUpdate(result.model))
                }
            }
        } else {
            postErrorState()
        }
    }

    private fun MediatorLiveData<BloggingPromptUpdate>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        isRefresh: Boolean? = null
    ) {
        when (isRefresh) {
            null, true -> refreshData(coroutineScope, siteLocalId)
            else -> Unit // Do nothing
        }
    }

    private fun MediatorLiveData<BloggingPromptUpdate>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ) {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            fetchPromptsAndPostErrorIfAvailable(coroutineScope, selectedSite)
        } else {
            postErrorState()
        }
    }

    private fun MediatorLiveData<BloggingPromptUpdate>.fetchPromptsAndPostErrorIfAvailable(
        coroutineScope: CoroutineScope,
        selectedSite: SiteModel
    ) {
        coroutineScope.launch(bgDispatcher) {
            delay(REFRESH_DELAY)

            val cal: Calendar = Calendar.getInstance()
            cal.time = Date()
            cal.add(Calendar.DATE, -10)
            val dateBefore10Days: Date = cal.time

            val result = promptsStore.fetchPrompts(selectedSite, 40, dateBefore10Days)
            val model = result.model
            val error = result.error
            when {
                error != null -> postErrorState()
                model != null -> onRefreshedBackgroundThread()
                else -> onRefreshedBackgroundThread()
            }
        }
    }

    private fun MediatorLiveData<BloggingPromptUpdate>.postErrorState() {
        postState(
                BloggingPromptUpdate(null)
        )
    }
}
