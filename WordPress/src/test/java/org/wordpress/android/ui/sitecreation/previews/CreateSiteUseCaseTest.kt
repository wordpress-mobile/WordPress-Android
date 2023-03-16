package org.wordpress.android.ui.sitecreation.previews

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility
import org.wordpress.android.ui.sitecreation.FREE_DOMAIN
import org.wordpress.android.ui.sitecreation.PAID_DOMAIN
import org.wordpress.android.ui.sitecreation.SITE_REMOTE_ID
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceData
import org.wordpress.android.ui.sitecreation.usecases.CreateSiteUseCase
import org.wordpress.android.util.UrlUtilsWrapper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val SITE_TITLE = "site title"
private val SITE_DATA_FREE = SiteCreationServiceData(
    123,
    "slug",
    FREE_DOMAIN.domainName,
    SITE_TITLE,
    FREE_DOMAIN.isFree,
)
private val SITE_DATA_PAID = SiteCreationServiceData(
    123,
    "slug",
    PAID_DOMAIN.domainName,
    SITE_TITLE,
    PAID_DOMAIN.isFree,
)
private const val LANGUAGE_ID = "lang_id"
private const val TIMEZONE_ID = "timezone_id"
private val EVENT = OnNewSiteCreated(newSiteRemoteId = SITE_REMOTE_ID)

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CreateSiteUseCaseTest : BaseUnitTest() {
    @Mock
    private lateinit var dispatcher: Dispatcher

    @Mock
    private lateinit var store: SiteStore

    @Mock
    private lateinit var urlUtilsWrapper: UrlUtilsWrapper
    private lateinit var useCase: CreateSiteUseCase

    @Before
    fun setUp() {
        useCase = CreateSiteUseCase(dispatcher, store, urlUtilsWrapper)
        whenever(dispatcher.dispatch(any())).then { useCase.onNewSiteCreated(EVENT) }
    }

    @Test
    fun coroutineResumedWhenResultEventDispatched() = test {
        val resultEvent = useCase.createSite(SITE_DATA_FREE, LANGUAGE_ID, TIMEZONE_ID)
        assertThat(resultEvent).isEqualTo(EVENT)
    }

    @Test
    fun verifySiteDataPropagated() = test {
        useCase.createSite(SITE_DATA_PAID, LANGUAGE_ID, TIMEZONE_ID)

        verify(dispatcher).dispatch(argPayload {
            assertEquals(SITE_DATA_PAID.domain, siteName)
            assertEquals(SITE_DATA_PAID.segmentId, segmentId)
            assertEquals(SITE_DATA_PAID.title, siteTitle)
            val findAvailableUrl = assertNotNull(findAvailableUrl)
            findAvailableUrl
        })
    }

    @Test
    fun verifySiteDataWhenFreePropagatesNoFindAvailableUrl() = test {
        whenever(urlUtilsWrapper.extractSubDomain(any())).thenReturn(SITE_DATA_FREE.domain)
        useCase.createSite(SITE_DATA_FREE, LANGUAGE_ID, TIMEZONE_ID)
        verify(dispatcher).dispatch(argPayload {
            assertEquals(SITE_DATA_FREE.domain, siteName)
            findAvailableUrl == null
        })
    }

    @Test
    fun verifyDryRunIsFalse() = test {
        useCase.createSite(SITE_DATA_FREE, LANGUAGE_ID, TIMEZONE_ID)
        verify(dispatcher).dispatch(argPayload { !dryRun })
    }

    @Test
    fun verifyCreatesPublicSite() = test {
        useCase.createSite(SITE_DATA_FREE, LANGUAGE_ID, TIMEZONE_ID)
        verify(dispatcher).dispatch(argPayload { visibility == SiteVisibility.PUBLIC })
    }

    @Test
    fun verifyPropagatesLanguageId() = test {
        useCase.createSite(SITE_DATA_FREE, LANGUAGE_ID, TIMEZONE_ID)
        verify(dispatcher).dispatch(argPayload { language == LANGUAGE_ID })
    }

    @Test
    fun verifyPropagatesTimeZoneId() = test {
        useCase.createSite(SITE_DATA_FREE, LANGUAGE_ID, TIMEZONE_ID)
        verify(dispatcher).dispatch(argPayload { timeZoneId == TIMEZONE_ID })
    }
}

fun argPayload(predicate: NewSitePayload.() -> Boolean) = argThat<Action<NewSitePayload>> { predicate(payload) }
