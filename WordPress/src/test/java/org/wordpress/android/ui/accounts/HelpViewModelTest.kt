package org.wordpress.android.ui.accounts

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.viewmodel.Event

@ExperimentalCoroutinesApi
class HelpViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var wordPress: WordPress

    @Mock
    private lateinit var onSignoutCompletedObserver: Observer<Unit>

    private val accountStore = mock<AccountStore>()
    private val siteStore = mock<SiteStore>()
    private val dispatcher = mock<Dispatcher>()

    private lateinit var viewModel: HelpViewModel

    @Before
    fun setUp() {
        viewModel = HelpViewModel(
            mainDispatcher = testDispatcher(),
            bgDispatcher = testDispatcher(),
            accountStore = accountStore,
            siteStore = siteStore,
            dispatcher = dispatcher,
        )
    }

    @Test
    fun `when log out is clicked, dialog is shown and sign out is invoked`() {
        val showSigningOutDialogEvents = mutableListOf<Event<Boolean>>()
        viewModel.showSigningOutDialog.observeForever { showSigningOutDialogEvents.add(it) }
        val onSignoutCompletedCaptor = ArgumentCaptor.forClass(Unit::class.java)
        viewModel.onSignOutCompleted.observeForever(onSignoutCompletedObserver)

        viewModel.signOutWordPress(wordPress)

        verify(wordPress).wordPressComSignOut()
        assertThat(showSigningOutDialogEvents[0].getContentIfNotHandled()).isTrue
        assertThat(showSigningOutDialogEvents[1].getContentIfNotHandled()).isFalse
        verify(onSignoutCompletedObserver).onChanged(onSignoutCompletedCaptor.capture())
    }

    @Test
    fun `when log out is clicked while logged in with site address, remove all sites action is dispatched`() {
        whenever(siteStore.hasSiteAccessedViaXMLRPC()).thenReturn(true)

        viewModel.signOutWordPress(wordPress)

        verify(dispatcher).dispatch(argThat { type == SiteAction.REMOVE_ALL_SITES })
    }

    @Test
    fun `when log out is clicked while logged in with wpcom, no action is dispatched`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        viewModel.signOutWordPress(wordPress)

        verifyNoInteractions(dispatcher)
    }
}
