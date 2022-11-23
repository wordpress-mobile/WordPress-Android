package org.wordpress.android.ui.deeplinks

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ContextProvider

@RunWith(MockitoJUnitRunner::class)
class DeepLinkingCustomIntentReceiverViewModelTest : BaseUnitTest() {
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var activityLauncherWrapper: ActivityLauncherWrapper
    @Mock lateinit var contextProvider: ContextProvider
    @Mock lateinit var context: Context
    private lateinit var viewModel: DeepLinkingCustomIntentReceiverViewModel

    @Before
    fun setUp() {
        whenever(contextProvider.getContext()).thenReturn(context)
        viewModel = DeepLinkingCustomIntentReceiverViewModel(
                analyticsTrackerWrapper,
                activityLauncherWrapper,
                contextProvider)
    }

    @Test
    fun `when incoming custom deep link received, then request is tracked`() {
        viewModel.forwardDeepLink(getDeepLinkIntent())

        verify(analyticsTrackerWrapper).track(eq(Stat.DEEPLINK_CUSTOM_INTENT_RECEIVED))
    }

    @Test
    fun `when incoming custom deep link received, then deep link request is forwarded`() {
        val deepLinkIntent = getDeepLinkIntent()

        viewModel.forwardDeepLink(deepLinkIntent)

        verify(activityLauncherWrapper).forwardDeepLinkIntent(contextProvider.getContext(), deepLinkIntent)
    }

    private fun getDeepLinkIntent() = Intent(context, DeepLinkingIntentReceiverActivity::class.java).apply {
        action = "action"
        data = Uri.EMPTY
    }
}
