package org.wordpress.android.ui.sitecreation.verticals

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.experimental.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsUseCase
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsHeaderUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsModelUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsSearchInputUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsUiState

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationVerticalsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var fetchVerticalsUseCase: FetchVerticalsUseCase

    private lateinit var viewModel: NewSiteCreationVerticalsViewModel

    @Mock private lateinit var uiStateObserver: Observer<VerticalsUiState>

    private val firstModel = VerticalModel("firstModel", "1")
    private val secondModel = VerticalModel("secondModel", "2")
    private val headerAndEmptyInputState = VerticalsUiState(
            false,
            true,
            true,
            listOf(VerticalsHeaderUiState("dummyTitle", "dummySubtitle"), VerticalsSearchInputUiState(false, false))
    )
    private val progressState = VerticalsUiState(false, true, false, listOf(VerticalsSearchInputUiState(true, true)))
    private val progressStateAfterFirstModelFetched = VerticalsUiState(
            false,
            true,
            false,
            listOf(
                    VerticalsSearchInputUiState(true, true),
                    VerticalsModelUiState(firstModel.verticalId, firstModel.name)
            )
    )
    private val firstModelDisplayedState = VerticalsUiState(
            false,
            true,
            false,
            listOf(
                    VerticalsSearchInputUiState(false, true),
                    VerticalsModelUiState(firstModel.verticalId, firstModel.name)
            )
    )

    private val secondModelDisplayedState = VerticalsUiState(
            false,
            true,
            false,
            listOf(
                    VerticalsSearchInputUiState(false, true),
                    VerticalsModelUiState(secondModel.verticalId, secondModel.name)
            )
    )

    private val firstModelEvent = OnVerticalsFetched("a", listOf(firstModel), null)
    private val secondModelEvent = OnVerticalsFetched("b", listOf(secondModel), null)

    @Before
    fun setUp() {
        viewModel = NewSiteCreationVerticalsViewModel(
                dispatcher,
                fetchVerticalsUseCase,
                Dispatchers.Unconfined,
                Dispatchers.Unconfined
        )
        val uiStateObservable = viewModel.uiState
        uiStateObservable.observeForever(uiStateObserver)
    }

    @Test
    fun verifyContentShownOnStart() = test {
        viewModel.start()
        assertTrue(viewModel.uiState.value!!.showContent)
        assertFalse(viewModel.uiState.value!!.showError)
    }

    @Test
    fun verifyHeaderShownOnStart() = test {
        viewModel.start()
        assertTrue(viewModel.uiState.value!!.items[0] is VerticalsHeaderUiState)
    }

    @Test
    fun verifyHeaderNotShownOnNonEmptyQuery() = test {
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(firstModelEvent)
        viewModel.start()
        viewModel.updateQuery("a",0)
        assertFalse(viewModel.uiState.value!!.items[0] is VerticalsHeaderUiState)
    }

    @Test
    fun verifyInputShownOnStart() = test {
        viewModel.start()
        assertTrue(viewModel.uiState.value!!.items[1] is VerticalsSearchInputUiState)
    }

    @Test
    fun verifyClearSearchNotShownOnStart() = test {
        viewModel.start()
        val searchState = viewModel.uiState.value!!.items[1] as VerticalsSearchInputUiState
        assertFalse(searchState.showClearButton)
    }

    @Test
    fun verifyClearSearchShownOnNoneEmptyQuery() = test {
        viewModel.start()
        viewModel.updateQuery("abc", 0)
        val searchState = viewModel.uiState.value!!.items[0] as VerticalsSearchInputUiState
        assertTrue(searchState.showClearButton)
    }

    @Test
    fun verifySearchProgressNotShownOnStart() = test {
        viewModel.start()
        val searchState = viewModel.uiState.value!!.items[1] as VerticalsSearchInputUiState
        assertFalse(searchState.showProgress)
    }

    @Test
    fun verifyStatesAfterUpdatingQuery() = test {
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(firstModelEvent)
        viewModel.start()
        viewModel.updateQuery("a", delay = 0)
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(secondModelEvent)
        viewModel.updateQuery("ab", delay = 0)

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(headerAndEmptyInputState)
            verify(uiStateObserver).onChanged(progressState)
            verify(uiStateObserver).onChanged(firstModelDisplayedState)
            verify(uiStateObserver).onChanged(progressStateAfterFirstModelFetched)
            verify(uiStateObserver).onChanged(secondModelDisplayedState)
            verifyNoMoreInteractions()
        }
    }

    // TODO error states
}
