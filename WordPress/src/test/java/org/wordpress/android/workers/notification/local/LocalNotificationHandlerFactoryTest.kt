package org.wordpress.android.workers.notification.local

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.workers.notification.bloggingprompts.BloggingPromptsOnboardingNotificationHandler
import org.wordpress.android.workers.notification.createsite.CreateSiteNotificationHandler
import org.wordpress.android.workers.notification.local.LocalNotification.Type.CREATE_SITE

@RunWith(MockitoJUnitRunner::class)
class LocalNotificationHandlerFactoryTest {
    @Mock
    lateinit var createSiteNotificationHandler: CreateSiteNotificationHandler

    @Mock
    lateinit var bloggingPromptsOnboardingNotificationHandler: BloggingPromptsOnboardingNotificationHandler

    lateinit var localNotificationHandlerFactory: LocalNotificationHandlerFactory

    @Before
    fun setUp() {
        localNotificationHandlerFactory = LocalNotificationHandlerFactory(
            createSiteNotificationHandler,
            bloggingPromptsOnboardingNotificationHandler
        )
    }

    @Test
    fun verifyLocalNotificationHandlerBuildsCorrectHandler() {
        val handler = localNotificationHandlerFactory.buildLocalNotificationHandler(CREATE_SITE)
        assertThat(handler).isInstanceOf(CreateSiteNotificationHandler::class.java)
    }
}
