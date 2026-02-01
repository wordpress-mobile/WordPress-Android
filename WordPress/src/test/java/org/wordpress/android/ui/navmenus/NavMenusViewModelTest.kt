package org.wordpress.android.ui.navmenus

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.navmenus.data.NavMenuRestClient

@ExperimentalCoroutinesApi
class NavMenusViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var navMenuRestClient: NavMenuRestClient

    private lateinit var viewModel: NavMenusViewModel

    private val testSite = SiteModel().apply {
        id = 123
        siteId = 456L
    }

    @Before
    fun setup() {
        viewModel = NavMenusViewModel(
            selectedSiteRepository = selectedSiteRepository,
            navMenuRestClient = navMenuRestClient,
            mainDispatcher = testDispatcher(),
            ioDispatcher = testDispatcher()
        )
    }

    @Test
    fun `when loadMenus called without site, then error state is set`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        viewModel.loadMenus()

        val state = viewModel.menuListState.first()
        assertThat(state.error).isEqualTo("No site selected")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `when navigateToCreateMenu called, then menu detail state is initialized for new menu`() = test {
        viewModel.navigateToCreateMenu()

        val state = viewModel.menuDetailState.first()
        assertThat(state).isNotNull
        assertThat(state?.menuId).isEqualTo(0L)
        assertThat(state?.isNew).isTrue()
        assertThat(state?.name).isEmpty()
    }

    @Test
    fun `when updateMenuName called, then menu name is updated`() = test {
        viewModel.navigateToCreateMenu()

        viewModel.updateMenuName("My Menu")

        val state = viewModel.menuDetailState.first()
        assertThat(state?.name).isEqualTo("My Menu")
    }

    @Test
    fun `when updateMenuDescription called, then description is updated`() = test {
        viewModel.navigateToCreateMenu()

        viewModel.updateMenuDescription("Menu description")

        val state = viewModel.menuDetailState.first()
        assertThat(state?.description).isEqualTo("Menu description")
    }

    @Test
    fun `when updateMenuAutoAdd called, then autoAdd is updated`() = test {
        viewModel.navigateToCreateMenu()

        viewModel.updateMenuAutoAdd(true)

        val state = viewModel.menuDetailState.first()
        assertThat(state?.autoAdd).isTrue()
    }

    @Test
    fun `when toggleMenuLocation called, then location is added`() = test {
        viewModel.navigateToCreateMenu()

        viewModel.toggleMenuLocation("primary")

        val state = viewModel.menuDetailState.first()
        assertThat(state?.selectedLocations).contains("primary")
    }

    @Test
    fun `when toggleMenuLocation called twice for same location, then location is removed`() = test {
        viewModel.navigateToCreateMenu()

        viewModel.toggleMenuLocation("primary")
        viewModel.toggleMenuLocation("primary")

        val state = viewModel.menuDetailState.first()
        assertThat(state?.selectedLocations).doesNotContain("primary")
    }

    @Test
    fun `when saveMenu called with empty name, then error event is emitted`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        viewModel.navigateToCreateMenu()

        viewModel.saveMenu()

        val event = viewModel.uiEvent.first()
        assertThat(event).isInstanceOf(NavMenusUiEvent.ShowError::class.java)
        assertThat((event as NavMenusUiEvent.ShowError).message).isEqualTo("Menu name is required")
    }

    @Test
    fun `when consumeUiEvent called, then event is cleared`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        viewModel.navigateToCreateMenu()
        viewModel.saveMenu() // triggers error event

        viewModel.consumeUiEvent()

        val event = viewModel.uiEvent.first()
        assertThat(event).isNull()
    }
}
