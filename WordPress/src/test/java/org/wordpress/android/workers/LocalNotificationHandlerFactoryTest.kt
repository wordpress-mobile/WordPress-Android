package org.wordpress.android.workers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.workers.LocalNotification.Type.CREATE_SITE

@RunWith(MockitoJUnitRunner::class)
class LocalNotificationHandlerFactoryTest {
    @Mock
    lateinit var createSiteNotificationHandler: CreateSiteNotificationHandler

    lateinit var localNotificationHandlerFactory: LocalNotificationHandlerFactory

    @Before
    fun setUp() {
        localNotificationHandlerFactory = LocalNotificationHandlerFactory(createSiteNotificationHandler)
    }

    @Test
    fun verifyLocalNotificationHandlerBuildsCorrectHandler() {
        val handler = localNotificationHandlerFactory.buildLocalNotificationHandler(CREATE_SITE)
        assertThat(handler).isInstanceOf(CreateSiteNotificationHandler::class.java)
    }
}
