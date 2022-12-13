package org.wordpress.android.ui.posts.prepublishing

import android.os.Bundle
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.store.TaxonomyStore
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyError
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType.GENERIC_ERROR
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.AddCategoryUseCase
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.GetCategoriesUseCase
import org.wordpress.android.ui.posts.PrepublishingAddCategoryRequest
import org.wordpress.android.ui.posts.PrepublishingCategoriesViewModel
import org.wordpress.android.ui.posts.PrepublishingCategoriesViewModel.UiState
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PrepublishingCategoriesViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrepublishingCategoriesViewModel
    @Mock lateinit var getCategoriesUseCase: GetCategoriesUseCase
    @Mock lateinit var addCategoryUseCase: AddCategoryUseCase
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var editPostRepository: EditPostRepository
    @Mock lateinit var siteModel: SiteModel

    @Before
    fun setup() = test {
        viewModel = PrepublishingCategoriesViewModel(
                getCategoriesUseCase,
                addCategoryUseCase,
                analyticsTrackerWrapper,
                networkUtilsWrapper,
                TEST_DISPATCHER
        )

        whenever(getCategoriesUseCase.getPostCategories(anyOrNull()))
                .thenReturn(postCategoriesList())
        whenever(getCategoriesUseCase.getSiteCategories(any()))
                .thenReturn(siteCategoriesList())
        whenever(addCategoryUseCase.addCategory(anyString(), anyLong(), any())).doAnswer { }
    }

    @Test
    fun `when viewModel is started updateToolbarTitle is called with the categories title`() {
        val toolbarTitleUiState = init().toolbarTitleUiState
        viewModel.start(editPostRepository, siteModel, null, listOf())

        val title: UiStringRes? = toolbarTitleUiState[0] as UiStringRes

        assertThat(title?.stringRes)
                .isEqualTo(R.string.prepublishing_nudges_toolbar_title_categories)
    }

    @Test
    fun `when viewModel is started add category button is visible on the titlebar`() {
        val uiStates = init().uiStates
        viewModel.start(editPostRepository, siteModel, null, listOf())

        val addCategoryButtonVisibility: Boolean = uiStates[0].addCategoryActionButtonVisibility

        Assertions.assertThat(addCategoryButtonVisibility)
                .isEqualTo(true)
    }

    @Test
    fun `when viewModel is started progress views is not visible when not an addCategoryRequest`() {
        val uiStates = init().uiStates
        viewModel.start(editPostRepository, siteModel, null, listOf())

        val progressVisibility: Boolean = uiStates[0].progressVisibility

        Assertions.assertThat(progressVisibility)
                .isEqualTo(false)
    }

    @Test
    fun `when viewModel is started progress views is visible when this is an addCategoryRequest`() {
        val uiStates = init().uiStates
        viewModel.start(editPostRepository, siteModel, addCategoryRequest, listOf())

        val progressVisibility: Boolean = uiStates[0].progressVisibility

        Assertions.assertThat(progressVisibility)
                .isEqualTo(true)
    }

    @Test
    fun `when onBackClicked is triggered navigateToHomeScreen is called`() {
        val navigateToHome = init().navigateToHome

        viewModel.start(editPostRepository, siteModel, null, listOf())
        viewModel.onBackButtonClick()

        assertThat(navigateToHome[0]).isNotNull
    }

    @Test
    fun `when onAddCategoryClick is triggered navigateToAddCategoryScreen is called`() {
        val navigateToAddCategoryScreen = init().navigateToAddCategoryScreen

        viewModel.start(editPostRepository, siteModel, null, listOf())
        viewModel.onAddCategoryClick()

        Assertions.assertThat(navigateToAddCategoryScreen[0]).isNotNull
    }

    @Test
    fun `getSiteCategories is invoked on start`() {
        viewModel.start(editPostRepository, siteModel, null, listOf())

        verify(getCategoriesUseCase, times(1)).getSiteCategories(siteModel)
    }

    @Test
    fun `getPostCategories is invoked on start`() {
        viewModel.start(editPostRepository, siteModel, null, listOf())

        verify(getCategoriesUseCase, times(1)).getPostCategories(editPostRepository)
    }

    @Test
    fun `fetchSiteCategories is invoked on start when not an addCategoryRequest`() {
        viewModel.start(editPostRepository, siteModel, null, listOf())

        verify(getCategoriesUseCase, times(1)).fetchSiteCategories(siteModel)
    }

    @Test
    fun `fetchSiteCategories is not invoked on start when there is an addCategoryRequest`() {
        viewModel.start(editPostRepository, siteModel, addCategoryRequest, listOf())

        verify(getCategoriesUseCase, never()).fetchSiteCategories(siteModel)
    }

    @Test
    fun `selected categories are correctly shown on successful start`() {
        val uiStates = init().uiStates
        viewModel.start(editPostRepository, siteModel, null, listOf())

        val siteCategories = siteCategoriesList()
        val postCategories = postCategoriesList()

        assertThat(viewModel.uiState.value).isNotNull
        assertThat(viewModel.uiState.value).isInstanceOf(UiState::class.java)
        assertThat(uiStates[0].categoriesListItemUiState.size).isEqualTo(siteCategories.size)
        val categoriesListItems =
                uiStates[0].categoriesListItemUiState
        assertThat(categoriesListItems.count { item ->
            item.checked
        }).isEqualTo(postCategories.size)
    }

    @Test
    fun `selected categories are correctly shown on successful start upon return from add `() {
        val uiStates = init().uiStates
        val postCategories = postCategoriesList()
        val siteCategories = siteCategoriesList()

        viewModel.start(editPostRepository, siteModel, null, postCategories)

        assertThat(viewModel.uiState.value).isNotNull
        assertThat(viewModel.uiState.value).isInstanceOf(UiState::class.java)
        assertThat(uiStates[0].categoriesListItemUiState.size).isEqualTo(siteCategories.size)
        val categoriesListItems =
                uiStates[0].categoriesListItemUiState
        assertThat(categoriesListItems.count { item ->
            item.checked
        }).isEqualTo(postCategories.size)
    }

    @Test
    fun `when AddCategory success response toast is shown`() {
        val msgs = init().snackbarMsgs
        viewModel.start(editPostRepository, siteModel, null, listOf())
        val termModel = getTermModel()
        val onTermUploaded = TaxonomyStore.OnTermUploaded(termModel)

        viewModel.onTermUploadedComplete(onTermUploaded)

        assertThat(msgs[0].peekContent().message).isEqualTo(UiStringRes(R.string.adding_cat_success))
    }

    @Test
    fun `when AddCategory success new item is checked in list`() {
        val uiStates = init().uiStates
        val siteCategories = updatedSiteCategoriesList()
        val selectedCategoriesCount = postCategoriesList().size + 1

        whenever(getCategoriesUseCase.getSiteCategories(any()))
                .thenReturn(siteCategoriesList())

        viewModel.start(editPostRepository, siteModel, null, listOf())
        val termModel = getTermModel()
        val onTermUploaded = TaxonomyStore.OnTermUploaded(termModel)

        whenever(getCategoriesUseCase.getSiteCategories(any()))
                .thenReturn(updatedSiteCategoriesList())
        viewModel.onTermUploadedComplete(onTermUploaded)

        assertThat(uiStates[1].categoriesListItemUiState.size).isEqualTo(siteCategories.size)
        val categoriesListItems =
                uiStates[1].categoriesListItemUiState
        assertThat(categoriesListItems.count { item ->
            item.checked
        }).isEqualTo(selectedCategoriesCount)
    }

    @Test
    fun `when addCategory fails response toast is shown`() {
        val msgs = init().snackbarMsgs
        viewModel.start(editPostRepository, siteModel, null, listOf())
        val onTermUploaded = TaxonomyStore.OnTermUploaded(getTermModel())
        onTermUploaded.error = TaxonomyError(GENERIC_ERROR, "This is an error")

        viewModel.onTermUploadedComplete(onTermUploaded)

        assertThat(msgs[0].peekContent().message).isEqualTo(UiStringRes(R.string.adding_cat_failed))
    }

    @Test
    fun `when no changes exist navigateToHomeScreen is called`() {
        val navigateToHome = init().navigateToHome
        viewModel.start(editPostRepository, siteModel, null, listOf())

        viewModel.onBackButtonClick()

        assertThat(navigateToHome[0]).isNotNull
    }

    @Test
    fun `when changes exist progress view is visible`() {
        val uiStates = init().uiStates
        viewModel.start(editPostRepository, siteModel, null, listOf())

        viewModel.start(editPostRepository, siteModel, null, listOf())
        val onTermUploaded = TaxonomyStore.OnTermUploaded(getTermModel())

        whenever(getCategoriesUseCase.getSiteCategories(any()))
                .thenReturn(updatedSiteCategoriesList())
        viewModel.onTermUploadedComplete(onTermUploaded)

        viewModel.onBackButtonClick()

        assertThat(uiStates[2].progressVisibility).isEqualTo(true)
    }

    private fun postCategoriesList() = listOf<Long>(1, 2, 3, 4, 5)
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

    private fun updatedSiteCategoriesList(): ArrayList<CategoryNode> {
        val termModel = getTermModel()
        val categoryNode = CategoryNode(termModel.remoteTermId, 0, termModel.name)

        val newList = arrayListOf<CategoryNode>()
        newList.addAll(siteCategoriesList())
        newList.add(categoryNode)
        return newList
    }

    private fun init(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever { uiStates.add(it) }

        val navigateToHome = mutableListOf<Event<Unit>>()
        viewModel.navigateToHomeScreen.observeForever { navigateToHome.add(it) }

        val navigateToAddCategoryScreen = mutableListOf<Bundle>()
        viewModel.navigateToAddCategoryScreen.observeForever { navigateToAddCategoryScreen.add(it) }

        val toolbarTitleUiState = mutableListOf<UiString>()
        viewModel.toolbarTitleUiState.observeForever { toolbarTitleUiState.add(it) }

        val msgs = mutableListOf<Event<SnackbarMessageHolder>>()
        viewModel.snackbarEvents.observeForever { msgs.add(it) }

        return Observers(
                uiStates,
                navigateToHome,
                navigateToAddCategoryScreen,
                toolbarTitleUiState,
                msgs
        )
    }

    // set up observers
    private data class Observers(
        val uiStates: List<UiState>,
        val navigateToHome: List<Event<Unit>>,
        val navigateToAddCategoryScreen: List<Bundle>,
        val toolbarTitleUiState: List<UiString>,
        val snackbarMsgs: List<Event<SnackbarMessageHolder>>
    )

    private val addCategoryRequest =
            PrepublishingAddCategoryRequest(
                    categoryText = "Flowers",
                    categoryParentId = 0
            )

    private fun getTermModel(): TermModel {
        val termModel = TermModel()
        termModel.name = "Cars"
        termModel.remoteTermId = 20
        termModel.slug = "Cars"
        return termModel
    }
}
