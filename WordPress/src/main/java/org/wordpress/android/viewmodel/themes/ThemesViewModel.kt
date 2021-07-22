package org.wordpress.android.viewmodel.themes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ThemeStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.THEMES
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetAction.Hide
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetAction.Show
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetUIState.Selection
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetUIState.Selection.KeepCurrentHomepage
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetUIState.Selection.UseThemeHomepage
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ThemesViewModel @Inject constructor(
    private val themeStore: ThemeStore,
    private val resourceProvider: ResourceProvider,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel
    private var isStarted = false

    private val _bottomSheetUiState = MutableLiveData<BottomSheetUIState>()
    val bottomSheetUiState: LiveData<BottomSheetUIState> = _bottomSheetUiState

    private val _bottomSheetAction = MutableLiveData<BottomSheetAction>()
    val bottomSheetAction: MutableLiveData<BottomSheetAction> = _bottomSheetAction

    fun start(site: SiteModel) {
        if (isStarted) return

        this.site = site
        _bottomSheetUiState.value = BottomSheetUIState()
        isStarted = true
    }

    private fun currentViewState(): BottomSheetUIState = bottomSheetUiState.value!!

    fun onActivateMenuItemClicked(themeId: String) {
        if (!site.isUsingWpComRestApi) {
            AppLog.i(THEMES, "Theme activation requires a site using WP.com REST API. Aborting request.")
            return
        }

        val theme = themeStore.getInstalledThemeByThemeId(site, themeId) ?: themeStore.getWpComThemeByThemeId(themeId)
        if (theme == null) {
            AppLog.w(THEMES, "Theme unavailable to activate. Fetch it and try again.")
            return
        }

        bottomSheetAction.value = Show
        setBottomSheetTexts(theme.name)
    }

    fun onUseThemeHomepageSelected() = setBottomSheetSelection(UseThemeHomepage)

    fun onKeepCurrentHomepageSelected() = setBottomSheetSelection(KeepCurrentHomepage)

    fun onPreviewButtonClicked() {
        // todo
    }

    fun onActivateButtonClicked() {
        // todo
    }

    fun onDismissButtonClicked() {
        bottomSheetAction.value = Hide
    }

    private fun setBottomSheetTexts(themeName: String) {
        _bottomSheetUiState.value = currentViewState().copy(
                themeNameText = themeName,
                sheetInfoText = resourceProvider.getString(R.string.theme_bottom_sheet_info, themeName),
                useThemeHomePageOptionText = resourceProvider
                        .getString(R.string.theme_bottom_sheet_use_theme_layout_main_text, themeName)
        )
    }

    private fun setBottomSheetSelection(selection: Selection) {
        _bottomSheetUiState.value = currentViewState().copy(selection = selection)
    }

    sealed class BottomSheetAction {
        object Show : BottomSheetAction()
        object Hide : BottomSheetAction()
    }

    data class BottomSheetUIState(
        val themeNameText: String = "",
        val sheetInfoText: String = "",
        val useThemeHomePageOptionText: String = "",
        val selection: Selection = KeepCurrentHomepage
    ) {
        sealed class Selection {
            object UseThemeHomepage : Selection()
            object KeepCurrentHomepage : Selection()
        }
    }
}
