package org.wordpress.android.ui.prefs.homepage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsDataLoader.LoadingResult.Error
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsDataLoader.LoadingResult.Loading
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsDataLoader.LoadingResult.Data
import javax.inject.Inject

class HomepageSettingsDataLoader
@Inject constructor(private val pageStore: PageStore) {
    suspend fun loadPages(
        siteModel: SiteModel
    ): Flow<LoadingResult> = flow {
        emit(Loading)
        emit(Data(loadPagesFromDb(siteModel)))
        if (pageStore.requestPagesFromServer(siteModel, false).isError) {
            emit(Error(R.string.site_settings_failed_to_load_pages))
        } else {
            emit(Data(loadPagesFromDb(siteModel)))
        }
    }

    private suspend fun loadPagesFromDb(siteModel: SiteModel) =
            pageStore.getPagesFromDb(siteModel).filter { it.status == PUBLISHED }

    sealed class LoadingResult {
        object Loading : LoadingResult()
        data class Error(val message: Int) : LoadingResult()
        data class Data(val pages: List<PageModel>) : LoadingResult()
    }
}
