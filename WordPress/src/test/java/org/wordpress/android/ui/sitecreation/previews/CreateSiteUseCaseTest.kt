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
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceData
import org.wordpress.android.ui.sitecreation.usecases.CreateSiteUseCase
import org.wordpress.android.util.UrlUtilsWrapper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val SITE_TITLE = "site title"
private val DUMMY_SITE_DATA: SiteCreationServiceData = SiteCreationServiceData(
    123,
    "slug",
    "domain",
    SITE_TITLE,
    false,
)
private const val LANGUAGE_ID = "lang_id"
private const val TIMEZONE_ID = "timezone_id"

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
    private lateinit var event: OnNewSiteCreated

    @Before
    fun setUp() {
        useCase = CreateSiteUseCase(dispatcher, store, urlUtilsWrapper)
        event = OnNewSiteCreated(newSiteRemoteId = 123)
        whenever(dispatcher.dispatch(any())).then { useCase.onNewSiteCreated(event) }
    }

    @Test
    fun coroutineResumedWhenResultEventDispatched() = test {
        val resultEvent = useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID)
        assertThat(resultEvent).isEqualTo(event)
    }

    @Test
    fun verifySiteDataPropagated() = test {
        useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID)

        verify(dispatcher).dispatch(argPayload {
            assertEquals(siteName, DUMMY_SITE_DATA.domain)
            assertEquals(segmentId, DUMMY_SITE_DATA.segmentId)
            assertEquals(siteTitle, SITE_TITLE)
            val findAvailableUrl = assertNotNull(findAvailableUrl)
            findAvailableUrl
        })
    }

    @Test
    fun verifySiteDataWhenFreePropagatesNoFindAvailableUrl() = test {
        useCase.createSite(DUMMY_SITE_DATA.copy(isFree = true), LANGUAGE_ID, TIMEZONE_ID)
        verify(dispatcher).dispatch(argPayload { findAvailableUrl == null })
    }

    @Test
    fun verifyDryRunIsFalse() = test {
        useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID)
        verify(dispatcher).dispatch(argPayload { !dryRun })
    }

    @Test
    fun verifyCreatesPublicSite() = test {
        useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID)
        verify(dispatcher).dispatch(argPayload { visibility == SiteVisibility.PUBLIC })
    }

    @Test
    fun verifyPropagatesLanguageId() = test {
        useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID)
        verify(dispatcher).dispatch(argPayload { language == LANGUAGE_ID })
    }

    @Test
    fun verifyPropagatesTimeZoneId() = test {
        useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID)
        verify(dispatcher).dispatch(argPayload { timeZoneId == TIMEZONE_ID })
    }
}

fun argPayload(predicate: NewSitePayload.() -> Boolean) = argThat<Action<NewSitePayload>> { predicate(payload) }
