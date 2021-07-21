package org.wordpress.android.viewmodel.themes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.store.ThemeStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.THEMES
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.themes.ThemesViewModel.Selection.KEEP_CURRENT_HOMEPAGE
import org.wordpress.android.viewmodel.themes.ThemesViewModel.Selection.USE_THEME_HOMEPAGE
import javax.inject.Inject
import javax.inject.Named

class ThemesViewModel @Inject constructor(
    private val themeStore: ThemeStore,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    enum class Selection {
        USE_THEME_HOMEPAGE,
        KEEP_CURRENT_HOMEPAGE
    }

    private lateinit var site: SiteModel

    private var _bottomSheetSelection = MutableLiveData<Event<Selection>>()
    val bottomSheetSelection: LiveData<Event<Selection>> = _bottomSheetSelection

    private var _showBottomSheet = MutableLiveData<Boolean>()
    val showBottomSheet: LiveData<Boolean> = _showBottomSheet

    private var _themeToActivate = MutableLiveData<ThemeModel>()
    val themeToActivate: LiveData<ThemeModel> = _themeToActivate

    fun start(site: SiteModel) {
        this.site = site
        _bottomSheetSelection.value = Event(KEEP_CURRENT_HOMEPAGE)
    }

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

        _showBottomSheet.value = true
        _themeToActivate.value = theme
    }

    fun onUseThemeHomepageSelected() {
        _bottomSheetSelection.value = Event(USE_THEME_HOMEPAGE)
    }

    fun onKeepCurrentHomepageSelected() {
        _bottomSheetSelection.value = Event(KEEP_CURRENT_HOMEPAGE)
    }

    fun onPreviewButtonClicked() {
        // todo
    }

    fun onActivateButtonClicked() {
        // todo
    }

    fun onDismissButtonClicked() {
        _showBottomSheet.value = false
    }
}
