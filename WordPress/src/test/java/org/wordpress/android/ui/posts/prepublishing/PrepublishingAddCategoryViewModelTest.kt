package org.wordpress.android.ui.posts.prepublishing

import android.os.Bundle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.test
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.GetCategoriesUseCase
import org.wordpress.android.ui.posts.PrepublishingAddCategoryViewModel
import org.wordpress.android.ui.posts.PrepublishingAddCategoryViewModel.UiState
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PrepublishingAddCategoryViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrepublishingAddCategoryViewModel
    @Mock lateinit var getCategoriesUseCase: GetCategoriesUseCase
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var siteModel: SiteModel

    @Before
    fun setup() = test {
        viewModel = PrepublishingAddCategoryViewModel(
                getCategoriesUseCase,
                networkUtilsWrapper,
                resourceProvider,
                TEST_DISPATCHER
        )

        whenever(getCategoriesUseCase.getSiteCategories(any()))
                .thenReturn(siteCategoriesList())
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `when viewModel is started updateToolbarTitle is called with the add category title`() {
        val toolbarTitleUiState = init().toolbarTitleUiState
        viewModel.start(siteModel, false)

        val title: UiStringRes? = toolbarTitleUiState[0] as UiStringRes

        assertThat(title?.stringRes)
                .isEqualTo(R.string.prepublishing_nudges_toolbar_title_add_categories)
    }

    @Test
    fun `when viewModel is started submit button is visible and not enabled`() {
        val uiStates = init().uiStates
        viewModel.start(siteModel, false)

        assertThat(uiStates[0].submitButtonUiState.enabled).isEqualTo(false)
        assertThat(uiStates[0].submitButtonUiState.visibility).isEqualTo(true)
    }

    @Test
    fun `when onSubmitClicked and there is no network a toast message is shown `() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        val snackbarMsgs = init().snackbarMsgs
        viewModel.start(siteModel, true)
        val newCategory = "Animals"
        viewModel.categoryNameUpdated(newCategory)
        viewModel.onSubmitButtonClick()

        assertThat(snackbarMsgs[0]).isNotNull
    }

    @Test
    fun `getSiteCategories is invoked on start`() {
        viewModel.start(siteModel, false)

        verify(getCategoriesUseCase, times(1)).getSiteCategories(siteModel)
    }

    @Test
    fun `when started category levels are loaded properly`() {
        val uiStates = init().uiStates
        val categoriesSize = siteCategoriesList().size
        viewModel.start(siteModel, false)

        // +1 is for the TopLevel
        assertThat(uiStates[0].categories.size).isEqualTo(categoriesSize + 1)
        assertThat(uiStates[0].selectedParentCategoryPosition).isEqualTo(0)
    }

    @Test
    fun `when text is entered into category name field submit button is enabled`() {
        val newCategory = "Animals"
        val uiStates = init().uiStates
        viewModel.start(siteModel, false)
        viewModel.categoryNameUpdated(newCategory)

        assertThat(uiStates[1].submitButtonUiState.enabled).isEqualTo(true)
    }

    @Test
    fun `when selected parent category the selected position is updated `() {
        val uiStates = init().uiStates
        val newSelectedPosition = 3
        viewModel.start(siteModel, false)
        viewModel.parentCategorySelected(newSelectedPosition)

        assertThat(uiStates[1].selectedParentCategoryPosition).isEqualTo(newSelectedPosition)
    }

    @Test
    fun `when category is entered the uiState is updated `() {
        val newCategory = "Animals"
        val uiStates = init().uiStates
        viewModel.start(siteModel, false)
        viewModel.categoryNameUpdated(newCategory)

        assertThat(uiStates[1].categoryName).isEqualTo(newCategory)
    }

    @Test
    fun `when submit button clicked with changes navigateBack is called`() {
        val navigateBack = init().navigateBack
        val newCategory = "Animals"
        viewModel.start(siteModel, false)
        viewModel.categoryNameUpdated(newCategory)
        viewModel.onSubmitButtonClick()

        assertThat(navigateBack[0]).isNotNull
    }

    private fun siteCategoriesList(): ArrayList<CategoryNode> {
        return arrayListOf(
                CategoryNode(1, 0, "Animals"),
                CategoryNode(2, 0, "Colors"),
                CategoryNode(3, 0, "Flavors"),
                CategoryNode(4, 0, "Articles"),
                CategoryNode(14, 4, "New"),
                CategoryNode(5, 0, "Fruit"),
                CategoryNode(6, 0, "Recipes"),
                CategoryNode(16, 6, "New")
        )
    }

    private fun init(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever { uiStates.add(it) }

        val navigateBack = mutableListOf<Bundle?>()
        viewModel.navigateBack.observeForever { navigateBack.add(it) }

        val dismissKeyboard = mutableListOf<Event<Unit>>()
        viewModel.dismissKeyboard.observeForever { dismissKeyboard.add(it) }

        val toolbarTitleUiState = mutableListOf<UiString>()
        viewModel.toolbarTitleUiState.observeForever { toolbarTitleUiState.add(it) }

        val msgs = mutableListOf<Event<SnackbarMessageHolder>>()
        viewModel.snackbarEvents.observeForever { msgs.add(it) }

        return Observers(
                uiStates,
                navigateBack,
                dismissKeyboard,
                toolbarTitleUiState,
                msgs
        )
    }

    // set up observers
    private data class Observers(
        val uiStates: List<UiState>,
        val navigateBack: List<Bundle?>,
        val dismissKeyboard: List<Event<Unit>>,
        val toolbarTitleUiState: List<UiString>,
        val snackbarMsgs: List<Event<SnackbarMessageHolder>>
    )
}
