package org.wordpress.android.ui.sitecreation.verticals

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker

@RunWith(MockitoJUnitRunner::class)
class SiteCreationIntentsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var analyticsTracker: SiteCreationTracker
    @Mock lateinit var dispatcher: CoroutineDispatcher

    private lateinit var viewModel: SiteCreationIntentsViewModel

    @Before
    fun setUp() {
        viewModel = SiteCreationIntentsViewModel(analyticsTracker, dispatcher)
    }

    @Test
    fun `when the screen starts an analytics event is emitted`() {
        viewModel.start()
        verify(analyticsTracker).trackSiteIntentQuestionViewed()
    }

    @Test
    fun `when the skip button is pressed an analytics event is emitted`() {
        viewModel.onSkipPressed()
        verify(analyticsTracker).trackSiteIntentQuestionSkipped()
    }

    @Test
    fun `when the back button is pressed an analytics event is emitted`() {
        viewModel.onBackPressed()
        verify(analyticsTracker).trackSiteIntentQuestionCanceled()
    }
}
