package org.wordpress.android.ui.main

import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.WordPress
import org.wordpress.android.models.recommend.RecommendApiCallsProvider
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
class MeViewModelTest : BaseUnitTest() {
    @Mock lateinit var wordPress: WordPress
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var recommendApiCallsProvider: RecommendApiCallsProvider
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var contextProvider: ContextProvider
    private lateinit var viewModel: MeViewModel

    @Before
    fun setUp() {
        viewModel = MeViewModel(
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                selectedSiteRepository,
                recommendApiCallsProvider,
                networkUtilsWrapper,
                contextProvider
        )
    }

    @Test
    fun `shows dialog and signs user out`() {
        val events = mutableListOf<Event<Boolean>>()
        viewModel.showDisconnectDialog.observeForever { events.add(it) }

        viewModel.signOutWordPress(wordPress)

        verify(wordPress).wordPressComSignOut()
        assertThat(events[0].getContentIfNotHandled()).isTrue()
        assertThat(events[1].getContentIfNotHandled()).isFalse()
    }

    @Test
    fun `opens disconnect dialog`() {
        val events = mutableListOf<Event<Boolean>>()
        viewModel.showDisconnectDialog.observeForever { events.add(it) }

        viewModel.openDisconnectDialog()

        assertThat(events[0].getContentIfNotHandled()).isTrue()
    }
}
