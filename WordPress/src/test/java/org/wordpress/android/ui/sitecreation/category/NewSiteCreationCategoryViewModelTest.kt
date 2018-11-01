package org.wordpress.android.ui.sitecreation.category

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.NewSiteCreationCategoryViewModel
import org.wordpress.android.ui.sitecreation.OnSiteCategoriesFetchedDummy
import org.wordpress.android.ui.sitecreation.usecases.FetchCategoriesUseCase

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationCategoryViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var fetchCategoriesUseCase: FetchCategoriesUseCase
    private val firstDummyEvent = OnSiteCategoriesFetchedDummy()
    private val secondDummyEvent = OnSiteCategoriesFetchedDummy()
    private val errorEvent = OnSiteCategoriesFetchedDummy(true, emptyList())
    private lateinit var viewModel: NewSiteCreationCategoryViewModel


    @Mock private lateinit var dataObserver: Observer<List<String>>
    @Mock private lateinit var showProgressObserver: Observer<Boolean>
    @Mock private lateinit var showErrorObserver: Observer<Boolean>

    @Before
    fun setUp() {
        viewModel = NewSiteCreationCategoryViewModel(
                dispatcher,
                fetchCategoriesUseCase,
                Dispatchers.Unconfined,
                Dispatchers.Unconfined
        )
        val dataObservable = viewModel.categories
        dataObservable.observeForever(dataObserver)
        val progressObservable = viewModel.showProgress
        progressObservable.observeForever(showProgressObserver)
        val errorObservable = viewModel.showError
        errorObservable.observeForever(showErrorObserver)
    }

    @Test
    fun onStartFetchesCategories() = test {
        whenever(fetchCategoriesUseCase.fetchCategories()).thenReturn(firstDummyEvent)
        viewModel.start()

        val inOrder = inOrder(dataObserver)
        inOrder.verify(dataObserver).onChanged(firstDummyEvent.data)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun onRetryFetchesCategories() = test {
        whenever(fetchCategoriesUseCase.fetchCategories()).thenReturn(firstDummyEvent)
        viewModel.start()
        whenever(fetchCategoriesUseCase.fetchCategories()).thenReturn(secondDummyEvent)
        viewModel.onRetryClicked()

        val inOrder = inOrder(dataObserver)
        inOrder.verify(dataObserver).onChanged(firstDummyEvent.data)

        inOrder.verify(dataObserver).onChanged(secondDummyEvent.data)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun fetchCategoriesChangesStateToProgress() = test {
        whenever(fetchCategoriesUseCase.fetchCategories()).thenReturn(firstDummyEvent)
        viewModel.start()

        val inOrder = inOrder(dataObserver, showProgressObserver, showErrorObserver)
        inOrder.verify(showProgressObserver).onChanged(true)
        inOrder.verify(dataObserver).onChanged(firstDummyEvent.data)
        inOrder.verify(showProgressObserver).onChanged(false)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun onRetryChangesStateToProgress() = test {
        whenever(fetchCategoriesUseCase.fetchCategories()).thenReturn(firstDummyEvent)
        viewModel.start()
        whenever(fetchCategoriesUseCase.fetchCategories()).thenReturn(secondDummyEvent)
        viewModel.onRetryClicked()

        val inOrder = inOrder(showProgressObserver, showErrorObserver)
        inOrder.verify(showProgressObserver).onChanged(true)
        inOrder.verify(showProgressObserver).onChanged(false)
        inOrder.verify(showProgressObserver).onChanged(true)
        inOrder.verify(showProgressObserver).onChanged(false)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun onErrorEventChangesStateToError() = test {
        whenever(fetchCategoriesUseCase.fetchCategories()).thenReturn(errorEvent)
        viewModel.start()

        val inOrder = inOrder(dataObserver, showProgressObserver, showErrorObserver)
        inOrder.verify(showProgressObserver).onChanged(true)
        inOrder.verify(showProgressObserver).onChanged(false)
        inOrder.verify(showErrorObserver).onChanged(true)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun onSuccessfulRetryRemovesErrorState() = test {
        whenever(fetchCategoriesUseCase.fetchCategories()).thenReturn(errorEvent)
        viewModel.start()
        whenever(fetchCategoriesUseCase.fetchCategories()).thenReturn(secondDummyEvent)
        viewModel.onRetryClicked()

        val inOrder = inOrder(dataObserver, showProgressObserver, showErrorObserver)
        inOrder.verify(showProgressObserver).onChanged(true)
        inOrder.verify(showProgressObserver).onChanged(false)
        inOrder.verify(showErrorObserver).onChanged(true)

        inOrder.verify(showProgressObserver).onChanged(true)
        inOrder.verify(showErrorObserver).onChanged(false)
        inOrder.verify(dataObserver).onChanged(secondDummyEvent.data)
        inOrder.verify(showProgressObserver).onChanged(false)
        inOrder.verifyNoMoreInteractions()
    }
}
