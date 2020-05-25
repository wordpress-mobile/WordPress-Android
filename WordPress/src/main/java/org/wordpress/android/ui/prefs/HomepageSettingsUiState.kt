package org.wordpress.android.ui.prefs

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.HomepageSettingsDataLoader.LoadingResult
import org.wordpress.android.ui.prefs.HomepageSettingsDataLoader.LoadingResult.Error
import org.wordpress.android.ui.prefs.HomepageSettingsDataLoader.LoadingResult.Loading
import org.wordpress.android.ui.prefs.HomepageSettingsDataLoader.LoadingResult.Success

data class HomepageSettingsUiState(
    val isClassicBlogState: Boolean,
    val siteModel: SiteModel,
    val isDisabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: Int? = null,
    val pageOnFrontModel: PageSelectorUiModel? = null,
    val pageForPostsModel: PageSelectorUiModel? = null,
    val retryAction: (() -> Unit)? = null
) {
    fun updateWithLoadingResult(
        loadingResult: LoadingResult,
        pageForPostsId: Long?,
        pageOnFrontId: Long?
    ): HomepageSettingsUiState {
        return when (loadingResult) {
            is Loading -> {
                this.copy(isLoading = true, error = null)
            }
            is Error -> this.copy(isLoading = false, error = loadingResult.message)
            is Success -> this.copy(
                    isLoading = false,
                    error = null,
                    pageForPostsModel = PageSelectorUiModel.build(loadingResult.pages, pageForPostsId),
                    pageOnFrontModel = PageSelectorUiModel.build(loadingResult.pages, pageOnFrontId)
            )
        }
    }

    fun updateWithError(message: Int): HomepageSettingsUiState {
        return this.copy(error = message, isLoading = false)
    }
}