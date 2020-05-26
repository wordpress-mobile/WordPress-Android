package org.wordpress.android.ui.prefs.homepage

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsDataLoader.LoadingResult
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsDataLoader.LoadingResult.Error
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsDataLoader.LoadingResult.Loading
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsDataLoader.LoadingResult.Data
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsUiState.ValidityResult.Invalid
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsUiState.ValidityResult.Valid

data class HomepageSettingsUiState(
    val isClassicBlogState: Boolean,
    val siteModel: SiteModel,
    val isSaveEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val error: Int? = null,
    val pageOnFrontState: HomepageSettingsSelectorUiState? = null,
    val pageForPostsState: HomepageSettingsSelectorUiState? = null
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
            is Data -> this.copy(
                    isLoading = false,
                    error = null,
                    pageForPostsState = HomepageSettingsSelectorUiState.Builder.build(
                            loadingResult.pages,
                            pageForPostsId
                    ),
                    pageOnFrontState = HomepageSettingsSelectorUiState.Builder.build(loadingResult.pages, pageOnFrontId)
            )
        }
    }

    fun updateWithError(message: Int): HomepageSettingsUiState {
        return this.copy(error = message, isLoading = false, isSaveEnabled = true)
    }

    fun updateWithLoading(): HomepageSettingsUiState {
        return this.copy(error = null, isLoading = true, isSaveEnabled = false)
    }

    fun updateWithPageForPosts(pageForPostsId: Int): HomepageSettingsUiState {
        return if (pageForPostsState != null) {
            val validityResult = validate(newPageForPostsId = pageForPostsId)
            copy(
                    pageForPostsState = pageForPostsState.selectItem(pageForPostsId),
                    error = validityResult.printErrorOrNull(),
                    isSaveEnabled = validityResult is Valid
            )
        } else {
            this
        }
    }

    fun updateWithPageOnFront(pageOnFrontId: Int): HomepageSettingsUiState {
        return if (pageOnFrontState != null) {
            val validityResult = validate(newPageOnFrontId = pageOnFrontId)
            copy(
                    pageOnFrontState = pageOnFrontState.selectItem(pageOnFrontId),
                    error = validityResult.printErrorOrNull(),
                    isSaveEnabled = validityResult is Valid
            )
        } else {
            this
        }
    }

    fun updateClassicBlogState(isClassicBlogState: Boolean): HomepageSettingsUiState {
        val validityResult = if (isClassicBlogState) Valid else validate()
        return copy(
                isClassicBlogState = isClassicBlogState,
                error = validityResult.printErrorOrNull(),
                isSaveEnabled = validityResult is Valid
        )
    }

    private fun validate(newPageForPostsId: Int? = null, newPageOnFrontId: Int? = null): ValidityResult {
        val pageForPostsId = newPageForPostsId ?: pageForPostsState?.selectedItemId
        val pageOnFrontId = newPageOnFrontId ?: pageOnFrontState?.selectedItemId
        return if (pageForPostsId == null || pageOnFrontId == null || pageForPostsId != pageOnFrontId) {
            Valid
        } else {
            Invalid(R.string.site_settings_page_for_posts_and_homepage_cannot_be_equal)
        }
    }

    sealed class ValidityResult {
        object Valid : ValidityResult()
        data class Invalid(val error: Int) : ValidityResult()

        fun printErrorOrNull(): Int? = (this as? Invalid)?.error
    }
}
