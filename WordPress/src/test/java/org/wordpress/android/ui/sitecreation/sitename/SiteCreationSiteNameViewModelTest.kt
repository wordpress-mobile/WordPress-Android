package org.wordpress.android.ui.sitecreation.sitename

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteCreationSiteNameViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var analyticsTracker: SiteCreationTracker

    @Mock
    lateinit var dispatcher: CoroutineDispatcher

    private lateinit var viewModel: SiteCreationSiteNameViewModel

    @Before
    fun setUp() {
        viewModel = SiteCreationSiteNameViewModel(analyticsTracker, dispatcher)
    }

    @Test
    fun `when the screen starts an analytics event is emitted`() {
        viewModel.start()
        verify(analyticsTracker).trackSiteNameViewed()
    }

    @Test
    fun `when the skip button is pressed an analytics event is emitted`() {
        viewModel.onSkipPressed()
        verify(analyticsTracker).trackSiteNameSkipped()
    }

    @Test
    fun `when the back button is pressed an analytics event is emitted`() {
        viewModel.onBackPressed()
        verify(analyticsTracker).trackSiteNameCanceled()
    }

    @Test
    fun `when the the site name is entered an analytics event is emitted`() {
        viewModel.onSiteNameChanged("site name")
        viewModel.onSiteNameEntered()
        verify(analyticsTracker).trackSiteNameEntered("site name")
    }

    @Test
    fun `when the the site name is blank the analytics event is not emitted`() {
        viewModel.onSiteNameChanged("")
        viewModel.onSiteNameEntered()
        verify(analyticsTracker, never()).trackSiteNameEntered(any())
    }
}
