package org.wordpress.android.ui.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.ui.prefs.HomepageSettingsDataLoader.LoadingResult.Loading
import org.wordpress.android.ui.prefs.HomepageSettingsDataLoader.LoadingResult.Success
import javax.inject.Inject

class HomepageSettingsDataLoader
@Inject constructor(private val pageStore: PageStore) {
    suspend fun loadPages(
        siteModel: SiteModel,
        pageOnFrontId: Long?,
        pageForPostsId: Long?
    ): Flow<LoadingResult> = flow {
        emit(Loading)
        pageStore.getPagesFromDb(siteModel).filter { it.status == PUBLISHED }.let { model ->
            if (model.isValid(pageOnFrontId, pageForPostsId)) {
                emit(Success(model))
            }
        }
        emit(fetchPages(siteModel))
    }

    suspend fun fetchPages(
        siteModel: SiteModel
    ): LoadingResult {
        return if (pageStore.requestPagesFromServer(siteModel).isError) {
            LoadingResult.Error(R.string.site_settings_failed_to_load_pages)
        } else {
            Success(pageStore.getPagesFromDb(siteModel).filter { it.status == PUBLISHED })
        }
    }

    private fun List<PageModel>.hasPage(remoteId: Long?): Boolean {
        return remoteId == null || this.any { it.remoteId == remoteId }
    }

    private fun List<PageModel>.isValid(
        pageOnFrontRemoteId: Long?,
        pageForPostsRemoteId: Long?
    ): Boolean {
        return this.isNotEmpty() && this.hasPage(pageOnFrontRemoteId) && this.hasPage(pageForPostsRemoteId)
    }

    sealed class LoadingResult {
        object Loading : LoadingResult()
        data class Error(val message: Int) : LoadingResult()
        data class Success(val pages: List<PageModel>) : LoadingResult()
    }
}
