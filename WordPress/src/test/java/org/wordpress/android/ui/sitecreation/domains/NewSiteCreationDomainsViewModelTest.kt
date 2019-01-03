package org.wordpress.android.ui.sitecreation.domains

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.ui.sitecreation.domains.NewSiteCreationDomainsViewModel.DomainsUiState
import org.wordpress.android.ui.sitecreation.usecases.FetchDomainsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationDomainsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var fetchDomainsUseCase: FetchDomainsUseCase
    @Mock private lateinit var uiStateObserver: Observer<DomainsUiState>
    @Mock private lateinit var createSiteBtnObserver: Observer<String>
    @Mock private lateinit var clearBtnObserver: Observer<Void>
    @Mock private lateinit var onHelpClickedObserver: Observer<Unit>
    @Mock private lateinit var onInputFocusRequestedObserver: Observer<Unit>
    @Mock private lateinit var networkUtils: NetworkUtilsWrapper

    private lateinit var viewModel: NewSiteCreationDomainsViewModel

    @Before
    fun setUp() {
        viewModel = NewSiteCreationDomainsViewModel(
                networkUtils = networkUtils,
                dispatcher = dispatcher,
                fetchDomainsUseCase = fetchDomainsUseCase,
                IO = TEST_DISPATCHER,
                MAIN = TEST_DISPATCHER
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.createSiteBtnClicked.observeForever(createSiteBtnObserver)
        viewModel.clearBtnClicked.observeForever(clearBtnObserver)
        viewModel.onHelpClicked.observeForever(onHelpClickedObserver)
        viewModel.onInputFocusRequested.observeForever(onInputFocusRequestedObserver)
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }
}
