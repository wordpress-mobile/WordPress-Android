package org.wordpress.android.ui.sitecreation.segments

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentsError
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentsFetched
import org.wordpress.android.fluxc.store.VerticalStore.VerticalErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentsUseCase

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationSegmentsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var mFetchSegmentsUseCase: FetchSegmentsUseCase
    private val firstModel = OnSegmentsFetched(
            listOf(
                    VerticalSegmentModel(
                            "dummyTitle",
                            "dummySubtitle",
                            "http://dummy.com",
                            123
                    )
            )
    )
    private val secondDummyEvent = OnSegmentsFetched(
            listOf(
                    VerticalSegmentModel(
                            "dummyTitle",
                            "dummySubtitle",
                            "http://dummy.com",
                            999
                    )
            )
    )
    private val errorEvent = OnSegmentsFetched(emptyList(), FetchSegmentsError(GENERIC_ERROR, "dummyError"))
    private lateinit var viewModel: NewSiteCreationSegmentsViewModel


    @Mock private lateinit var dataObserver: Observer<List<VerticalSegmentModel>>
    @Mock private lateinit var showProgressObserver: Observer<Boolean>
    @Mock private lateinit var showErrorObserver: Observer<Boolean>

    @Before
    fun setUp() {
        viewModel = NewSiteCreationSegmentsViewModel(
                dispatcher,
                mFetchSegmentsUseCase,
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
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(firstModel)
        viewModel.start()

        val inOrder = inOrder(dataObserver)
        inOrder.verify(dataObserver).onChanged(firstModel.segmentList)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun onRetryFetchesCategories() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(firstModel)
        viewModel.start()
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(secondDummyEvent)
        viewModel.onRetryClicked()

        val inOrder = inOrder(dataObserver)
        inOrder.verify(dataObserver).onChanged(firstModel.segmentList)

        inOrder.verify(dataObserver).onChanged(secondDummyEvent.segmentList)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun fetchCategoriesChangesStateToProgress() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(firstModel)
        viewModel.start()

        val inOrder = inOrder(dataObserver, showProgressObserver, showErrorObserver)
        inOrder.verify(showProgressObserver).onChanged(true)
        inOrder.verify(dataObserver).onChanged(firstModel.segmentList)
        inOrder.verify(showProgressObserver).onChanged(false)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun onRetryChangesStateToProgress() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(firstModel)
        viewModel.start()
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(secondDummyEvent)
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
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(errorEvent)
        viewModel.start()

        val inOrder = inOrder(dataObserver, showProgressObserver, showErrorObserver)
        inOrder.verify(showProgressObserver).onChanged(true)
        inOrder.verify(showProgressObserver).onChanged(false)
        inOrder.verify(showErrorObserver).onChanged(true)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun onSuccessfulRetryRemovesErrorState() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(errorEvent)
        viewModel.start()
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(secondDummyEvent)
        viewModel.onRetryClicked()

        val inOrder = inOrder(dataObserver, showProgressObserver, showErrorObserver)
        inOrder.verify(showProgressObserver).onChanged(true)
        inOrder.verify(showProgressObserver).onChanged(false)
        inOrder.verify(showErrorObserver).onChanged(true)

        inOrder.verify(showProgressObserver).onChanged(true)
        inOrder.verify(showErrorObserver).onChanged(false)
        inOrder.verify(dataObserver).onChanged(secondDummyEvent.segmentList)
        inOrder.verify(showProgressObserver).onChanged(false)
        inOrder.verifyNoMoreInteractions()
    }
}
