package org.wordpress.android.ui.prefs.categories.detail

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TaxonomyAction.REMOVE_TERM
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyError
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType.GENERIC_ERROR
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.AddCategoryUseCase
import org.wordpress.android.ui.posts.DeleteCategoryUseCase
import org.wordpress.android.ui.posts.EditCategoryUseCase
import org.wordpress.android.ui.posts.GetCategoriesUseCase
import org.wordpress.android.ui.prefs.categories.detail.CategoryUpdateUiState.Failure
import org.wordpress.android.ui.prefs.categories.detail.CategoryUpdateUiState.InProgress
import org.wordpress.android.ui.prefs.categories.detail.CategoryUpdateUiState.Success
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class CategoryDetailViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CategoryDetailViewModel

    private val dispatcher: Dispatcher = mock()
    private val networkUtilsWrapper: NetworkUtilsWrapper = mock()
    private val getCategoriesUseCase: GetCategoriesUseCase = mock()
    private val addCategoryUseCase: AddCategoryUseCase = mock()
    private val editCategoryUseCase: EditCategoryUseCase = mock()
    private val deleteCategoryUseCase: DeleteCategoryUseCase = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val selectedSiteRepository: SelectedSiteRepository = mock()

    private val uiStates = mutableListOf<UiState>()
    private val onCategoryPushStates = mutableListOf<Event<CategoryUpdateUiState>>()

    private val siteModel: SiteModel = mock()
    private val topLevelCategory = "Top level"

    @Before
    fun setUp() {
        setUpMockResponse()
        viewModel = CategoryDetailViewModel(
                TEST_DISPATCHER,
                networkUtilsWrapper,
                getCategoriesUseCase,
                addCategoryUseCase,
                editCategoryUseCase,
                deleteCategoryUseCase,
                resourceProvider,
                dispatcher,
                selectedSiteRepository
        )
        setUpUiStateObservers()
    }

    private fun setUpUiStateObservers() {
        viewModel.uiState.observeForever { if (it != null) uiStates += it }
        viewModel.onCategoryPush.observeForever { if (it != null) onCategoryPushStates += it }
    }

    private fun setUpMockResponse() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(resourceProvider.getString(R.string.top_level_category_name)).thenReturn(topLevelCategory)
        whenever(getCategoriesUseCase.getSiteCategories(siteModel)).thenReturn(arrayListOf())
    }

    @Test
    fun `when vm starts, then default parent category is selected`() {
        viewModel.start()
        assertThat(topLevelCategory).isEqualTo(uiStates.first().categories[0].name)
    }

    @Test
    fun `when vm starts, then submit button is shown and disabled`() {
        viewModel.start()

        assertThat(uiStates.first().submitButtonUiState.enabled).isFalse
        assertThat(uiStates.first().submitButtonUiState.buttonText).isEqualTo(UiStringRes(R.string.add_new_category))
    }

    @Test
    fun `given category name is updated, when vm starts, then submit button is enabled`() {
        viewModel.start()
        viewModel.onCategoryNameUpdated("category name")

        assertThat(uiStates.last().submitButtonUiState.enabled).isTrue
    }

    @Test
    fun ` given parent category is updated, when vm starts, then ui state is updated`() {
        val categoryNode = CategoryNode(1, 1, "Category")
        whenever(getCategoriesUseCase.getSiteCategories(siteModel)).thenReturn(arrayListOf(categoryNode))

        viewModel.start()
        val selectedCategoryParent = 2
        viewModel.onParentCategorySelected(selectedCategoryParent)

        assertThat(selectedCategoryParent).isEqualTo(uiStates.last().selectedParentCategoryPosition)
    }

    @Test
    fun `given no internet, when submit is invoked, then no network message is shown`() {
        val categoryName = "Category name"

        viewModel.start()
        viewModel.onCategoryNameUpdated(categoryName)
        viewModel.onSubmitButtonClick()

        assertThat(Failure(UiStringRes(R.string.no_network_message)))
                .isEqualTo(onCategoryPushStates[0].peekContent() as Failure)
    }

    @Test
    fun `given internet available, when submit is invoked, then add category is invoked`() {
        val categoryName = "Category name"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        viewModel.start()
        viewModel.onCategoryNameUpdated(categoryName)
        viewModel.onSubmitButtonClick()

        assertThat(InProgress(R.string.adding_cat)).isEqualTo(onCategoryPushStates[0].peekContent())
        verify(addCategoryUseCase).addCategory(categoryName, 0, siteModel)
    }

    @Test
    fun `given api success, when submit is invoked, then success message is shown`() {
        viewModel.start()
        viewModel.onTermUploaded(getTermUploadSuccess())

        assertThat(Success(UiStringRes(R.string.adding_cat_success)))
                .isEqualTo(onCategoryPushStates[0].peekContent() as Success)
    }

    @Test
    fun `given api error, when submit is invoked, then error message is shown`() {
        viewModel.start()
        viewModel.onTermUploaded(getTermUploadError())

        assertThat(Failure(UiStringRes(R.string.adding_cat_failed)))
                .isEqualTo(onCategoryPushStates[0].peekContent() as Failure)
    }

    // Edit category tests
    @Test
    fun `when vm starts in edit mode, then parent category is selected`() {
        val siteCategories = getDummySiteCategories()
        val siteCategoriesAsNode  = getCategoriesAsNode()
        whenever(getCategoriesUseCase.getCategoriesForSite(siteModel)).thenReturn(siteCategories)
        whenever(getCategoriesUseCase.getSiteCategories(siteModel)).thenReturn(siteCategoriesAsNode)

        viewModel.start(14L)

        assertThat(uiStates.first().categories[0].name).isEqualTo(siteCategoriesAsNode[0].name)
    }

    @Test
    fun `when vm starts in edit mode, then submit button is shown and disabled`() {
        val siteCategories = getDummySiteCategories()
        whenever(getCategoriesUseCase.getCategoriesForSite(siteModel)).thenReturn(siteCategories)

        viewModel.start(14L)

        assertThat(uiStates.first().submitButtonUiState.enabled).isFalse
        assertThat(uiStates.first().submitButtonUiState.buttonText).isEqualTo(UiStringRes(R.string.update_category))
    }

    @Test
    fun `given category name is updated in edit mode, when vm starts, then submit button is enabled`() {
        viewModel.start()
        viewModel.onCategoryNameUpdated("category name")

        assertThat(uiStates.last().submitButtonUiState.enabled).isTrue
    }

    @Test
    fun ` given parent category while editing categories, when vm starts, then ui state is updated`() {
        val siteCategories = getDummySiteCategories()
        whenever(getCategoriesUseCase.getCategoriesForSite(siteModel)).thenReturn(siteCategories)

        viewModel.start(14L)
        val selectedCategoryParent = 2
        viewModel.onParentCategorySelected(selectedCategoryParent)

        assertThat(selectedCategoryParent).isEqualTo(uiStates.last().selectedParentCategoryPosition)
    }

    @Test
    fun `given no internet while editing categories, when submit is invoked, then no network message is shown`() {
        val siteCategories = getDummySiteCategories()
        whenever(getCategoriesUseCase.getCategoriesForSite(siteModel)).thenReturn(siteCategories)

        viewModel.start(14L)
        viewModel.onCategoryNameUpdated("New category name from test")
        viewModel.onSubmitButtonClick()

        assertThat(Failure(UiStringRes(R.string.no_network_message)))
                .isEqualTo(onCategoryPushStates[0].peekContent() as Failure)
    }

    @Test
    fun `given internet available while editing categories, when submit is invoked, then edit category is invoked`() {
        val siteCategories = getDummySiteCategories()
        val siteCategoriesAsNode  = getCategoriesAsNode()
        whenever(getCategoriesUseCase.getCategoriesForSite(siteModel)).thenReturn(siteCategories)
        whenever(getCategoriesUseCase.getSiteCategories(siteModel)).thenReturn(siteCategoriesAsNode)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val updatedCategoryName = "New category name from test"

        viewModel.start(14L)
        viewModel.onCategoryNameUpdated(updatedCategoryName)
        viewModel.onSubmitButtonClick()

        assertThat(InProgress(R.string.updating_cat)).isEqualTo(onCategoryPushStates[0].peekContent())
        verify(editCategoryUseCase).editCategory(
                14L, "dog",
                updatedCategoryName, 1, siteModel
        )
    }

    @Test
    fun `given api success, when submit is invoked while editing categories, then success message is shown`() {
        val siteCategories = getDummySiteCategories()
        whenever(getCategoriesUseCase.getCategoriesForSite(siteModel)).thenReturn(siteCategories)

        viewModel.start(14L)
        viewModel.onTermUploaded(getTermUploadSuccess())

        assertThat(Success(UiStringRes(R.string.updating_cat_success)))
                .isEqualTo(onCategoryPushStates[0].peekContent() as Success)
    }

    @Test
    fun `given api error while editing categories, when submit is invoked, then error message is shown`() {
        val siteCategories = getDummySiteCategories()
        whenever(getCategoriesUseCase.getCategoriesForSite(siteModel)).thenReturn(siteCategories)

        viewModel.start(14L)
        viewModel.onTermUploaded(getTermUploadError())

        assertThat(Failure(UiStringRes(R.string.updating_cat_failed)))
                .isEqualTo(onCategoryPushStates[0].peekContent() as Failure)
    }

    // Delete category use case
    @Test
    fun `given no internet while deleting categories, when submit is invoked, then no network message is shown`() {
        val siteCategories = getDummySiteCategories()
        whenever(getCategoriesUseCase.getCategoriesForSite(siteModel)).thenReturn(siteCategories)

        viewModel.start(14L)
        viewModel.deleteCategory()

        assertThat(Failure(UiStringRes(R.string.no_network_message)))
                .isEqualTo(onCategoryPushStates[0].peekContent() as Failure)
    }

    @Test
    fun `given internet available while delete categories, when submit is invoked, then delete category is invoked`() {
        val siteCategories = getDummySiteCategories()
        val siteCategoriesAsNode  = getCategoriesAsNode()
        whenever(getCategoriesUseCase.getCategoriesForSite(siteModel)).thenReturn(siteCategories)
        whenever(getCategoriesUseCase.getSiteCategories(siteModel)).thenReturn(siteCategoriesAsNode)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        viewModel.start(14L)
        viewModel.deleteCategory()

        assertThat(InProgress(R.string.deleting_cat)).isEqualTo(onCategoryPushStates[0].peekContent())
        verify(deleteCategoryUseCase).deleteCategory(
                siteCategories[1], siteModel
        )
    }

    @Test
    fun `given api success, when submit is invoked while deleting category, then success message is shown`() {
        val siteCategories = getDummySiteCategories()
        whenever(getCategoriesUseCase.getCategoriesForSite(siteModel)).thenReturn(siteCategories)

        viewModel.start(14L)
        viewModel.onTaxonomyChanged(getTaxonomyChangedCallback())

        assertThat(Success(UiStringRes(R.string.deleting_cat_success)))
                .isEqualTo(onCategoryPushStates[0].peekContent() as Success)
    }

    @Test
    fun `given api error while deleting category, when submit is invoked, then error message is shown`() {
        val siteCategories = getDummySiteCategories()
        whenever(getCategoriesUseCase.getCategoriesForSite(siteModel)).thenReturn(siteCategories)

        viewModel.start(14L)
        viewModel.onTaxonomyChanged(getTaxonomyChangedErrorCallback())

        assertThat(Failure(UiStringRes(R.string.deleting_cat_failed)))
                .isEqualTo(onCategoryPushStates[0].peekContent() as Failure)
    }


    private fun getDummySiteCategories(): java.util.ArrayList<TermModel> {
        val siteCategories = java.util.ArrayList<TermModel>()
        siteCategories.add(getParentTermModel())
        siteCategories.add(getChildTermModel())
        return siteCategories
    }

    private fun getCategoriesAsNode(): ArrayList<CategoryNode> {
        return arrayListOf(
                CategoryNode(1, 0, "Animals"),
                CategoryNode(14, 1, "Dog"),
        )
    }

    private fun getParentTermModel(): TermModel {
        val termModel = TermModel()
        termModel.name = "Animals"
        termModel.remoteTermId = 1
        termModel.slug = "Animals"
        return termModel
    }

    private fun getChildTermModel(): TermModel {
        val termModel = TermModel()
        termModel.name = "Dog"
        termModel.remoteTermId = 14
        termModel.parentRemoteId = 1
        termModel.slug = "dog"
        return termModel
    }

    private fun getTermUploadSuccess() = OnTermUploaded(TermModel())

    private fun getTermUploadError(): OnTermUploaded {
        val event = OnTermUploaded(TermModel())
        event.error = TaxonomyError(GENERIC_ERROR)
        return event
    }
    private fun getTaxonomyChangedCallback(): OnTaxonomyChanged {
        val taxonomyChanged = OnTaxonomyChanged(0)
        taxonomyChanged.causeOfChange = REMOVE_TERM
        return taxonomyChanged
    }

    private fun getTaxonomyChangedErrorCallback(): OnTaxonomyChanged {
        val taxonomyChanged = OnTaxonomyChanged(0)
        taxonomyChanged.causeOfChange = REMOVE_TERM
        taxonomyChanged.error = TaxonomyError(GENERIC_ERROR)
        return taxonomyChanged
    }
}
