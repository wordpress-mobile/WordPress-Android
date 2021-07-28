package org.wordpress.android.viewmodel.themes

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.ThemeActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.store.ThemeStore
import org.wordpress.android.fluxc.store.ThemeStore.ActivateThemePayload
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetAction
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetAction.Hide
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetAction.Preview
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetAction.Show
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetUIState

private const val THEME_ID = "theme-id"
private const val THEME_NAME = "Awesome Theme"
private const val RESOURCE_STRING = "This is a resource string"

@InternalCoroutinesApi
class ThemesViewModelTest : BaseUnitTest() {
    @Mock lateinit var themeStore: ThemeStore
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var fluxCDispatcher: Dispatcher
    private val coroutineDispatcher = TEST_DISPATCHER
    private lateinit var viewModel: ThemesViewModel

    val state: BottomSheetUIState get() = viewModel.bottomSheetUiState.value!!
    val action: BottomSheetAction? get() = viewModel.bottomSheetAction.value?.peekContent()

    @Before
    fun setUp() {
        viewModel = ThemesViewModel(
                themeStore,
                resourceProvider,
                fluxCDispatcher,
                coroutineDispatcher
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
        assertTrue(state.themeHomepageCheckmarkVisible)
    }

    @Test
    fun `when keep current homepage selected, then correct selection is made in UI state`() {
        // Act
        viewModel.onKeepCurrentHomepageSelected()

        // Assert
        assertTrue(state.currentHomepageCheckmarkVisible)
    }

    @Test
    fun `when dismiss button clicked, then bottom sheet is hidden`() {
        // Act
        viewModel.onDismissButtonClicked()

        // Assert
        assertThat(action).isInstanceOf(Hide::class.java)
    }

    @Test
    fun `given theme to activate, when preview button clicked, then preview action is fired`() {
        // Arrange
        setupTheme()
        startViewModel()
        viewModel.onActivateMenuItemClicked(THEME_ID)

        // Act
        viewModel.onPreviewButtonClicked()

        // Assert
        assertThat(action).isInstanceOf(Preview::class.java)
        assertEquals((action as Preview).themeId, THEME_ID)
    }

    @Test
    fun `given no theme to activate, when preview button clicked, then do nothing`() {
        // Act
        viewModel.onPreviewButtonClicked()

        // Assert
        assertNull(action)
    }

    @Test
    fun `given keep current homepage selected, when activate button clicked, then dispatch true in action`() {
        // Arrange
        val activateAction = setupActivateThemeAction(true)

        // Act
        viewModel.onActivateButtonClicked()

        // Assert
        verifyActivateThemeAction(activateAction)
    }

    @Test
    fun `given use theme homepage selected, when activate button clicked, then dispatch false in action`() {
        // Arrange
        val activateAction = setupActivateThemeAction(false)

        // Act
        viewModel.onActivateButtonClicked()

        // Assert
        verifyActivateThemeAction(activateAction)
    }

    @Test
    fun `given no theme to activate, when activate button clicked, then do nothing`() {
        // Act
        viewModel.onActivateButtonClicked()

        // Assert
        verify(fluxCDispatcher, never()).dispatch(any())
        assertNull(action)
    }

    private fun startViewModel() {
        viewModel.start(site)
    }

    private fun setupResourceProvider() {
        whenever(resourceProvider.getString(any(), any())).thenReturn(RESOURCE_STRING)
    }

    private fun setupTheme(themeName: String = THEME_NAME): ThemeModel {
        val theme = ThemeModel()
        theme.themeId = THEME_ID
        theme.name = themeName
        whenever(site.isUsingWpComRestApi).thenReturn(true)
        whenever(themeStore.getInstalledThemeByThemeId(site, THEME_ID)).thenReturn(theme)
        return theme
    }

    private fun setupActivateThemeAction(dontChangeHomepage: Boolean): Action<ActivateThemePayload> {
        val theme = setupTheme()
        val activateAction = ThemeActionBuilder.newActivateThemeAction(
                ActivateThemePayload(site, theme, dontChangeHomepage)
        )
        startViewModel()
        viewModel.onActivateMenuItemClicked(THEME_ID)
        when (dontChangeHomepage) {
            true -> viewModel.onKeepCurrentHomepageSelected()
            false -> viewModel.onUseThemeHomepageSelected()
        }

        return activateAction
    }

    private fun verifyActivateThemeAction(fluxCAction: Action<ActivateThemePayload>) {
        verify(fluxCDispatcher).dispatch(check { actionParam: Action<ActivateThemePayload> ->
            val actual = actionParam.payload
            val expected = fluxCAction.payload
            assertThat(actual.dontChangeHomepage).isEqualTo(expected.dontChangeHomepage)
            assertThat(actual.theme).isEqualTo(expected.theme)
            assertThat(actual.site).isEqualTo(expected.site)
        })
        assertThat(action).isInstanceOf(Hide::class.java)
    }
}
