package org.wordpress.android.viewmodel.themes

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.store.ThemeStore
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetAction
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetAction.Hide
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetAction.Show
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetUIState
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetUIState.Selection.KeepCurrentHomepage
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetUIState.Selection.UseThemeHomepage

private const val THEME_ID = "theme-id"
private const val THEME_NAME = "Awesome Theme"
private const val RESOURCE_STRING = "This is a resource string"

@InternalCoroutinesApi
class ThemesViewModelTest : BaseUnitTest() {
    @Mock lateinit var themeStore: ThemeStore
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var site: SiteModel
    private lateinit var viewModel: ThemesViewModel

    val state: BottomSheetUIState get() = viewModel.bottomSheetUiState.value!!
    val action: BottomSheetAction? get() = viewModel.bottomSheetAction.value

    @Before
    fun setUp() {
        viewModel = ThemesViewModel(
                themeStore,
                resourceProvider,
                TEST_DISPATCHER
        )
        startViewModel()
        setupResourceProvider()
    }

    @Test
    fun `given WPCom api not used, when activating theme, then don't show bottom sheet`() {
        // Arrange
        whenever(site.isUsingWpComRestApi).thenReturn(false)

        // Act
        viewModel.onActivateMenuItemClicked(THEME_ID)

        // Assert
        assertNull(action)
    }

    @Test
    fun `given theme not found in installed themes, when activating theme, then search in WPCom themes`() {
        // Arrange
        whenever(site.isUsingWpComRestApi).thenReturn(true)
        whenever(themeStore.getInstalledThemeByThemeId(site, THEME_ID)).thenReturn(null)

        // Act
        viewModel.onActivateMenuItemClicked(THEME_ID)

        // Assert
        verify(themeStore).getWpComThemeByThemeId(THEME_ID)
    }

    @Test
    fun `given theme found, when activating theme, then bottom sheet is shown`() {
        // Arrange
        setupTheme()

        // Act
        viewModel.onActivateMenuItemClicked(THEME_ID)

        // Assert
        assertThat(action).isInstanceOf(Show::class.java)
    }

    @Test
    fun `given theme found, when activating theme, then texts set in UI state`() {
        // Arrange
        setupTheme()

        // Act
        viewModel.onActivateMenuItemClicked(THEME_ID)

        // Assert
        assertEquals(state.themeNameText, THEME_NAME)
        assertEquals(state.sheetInfoText, RESOURCE_STRING)
        assertEquals(state.useThemeHomePageOptionText, RESOURCE_STRING)
    }

    @Test
    fun `when use theme homepage selected, then correct selection is made in UI state`() {
        // Act
        viewModel.onUseThemeHomepageSelected()

        // Assert
        assertThat(state.selection).isInstanceOf(UseThemeHomepage::class.java)
    }

    @Test
    fun `when keep current homepage selected, then correct selection is made in UI state`() {
        // Act
        viewModel.onKeepCurrentHomepageSelected()

        // Assert
        assertThat(state.selection).isInstanceOf(KeepCurrentHomepage::class.java)
    }

    @Test
    fun `when dismiss button clicked, then bottom sheet is hidden`() {
        // Act
        viewModel.onDismissButtonClicked()

        // Assert
        val action = viewModel.bottomSheetAction.value
        assertThat(action).isInstanceOf(Hide::class.java)
    }

    private fun startViewModel() {
        viewModel.start(site)
    }

    private fun setupResourceProvider() {
        whenever(resourceProvider.getString(any(), any())).thenReturn(RESOURCE_STRING)
    }

    private fun setupTheme(themeName: String = THEME_NAME) {
        val theme = ThemeModel()
        theme.name = themeName
        whenever(site.isUsingWpComRestApi).thenReturn(true)
        whenever(themeStore.getInstalledThemeByThemeId(site, THEME_ID)).thenReturn(theme)
    }
}
