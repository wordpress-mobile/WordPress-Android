package org.wordpress.android.ui.sitecreation.previews

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceData
import org.wordpress.android.ui.sitecreation.usecases.CreateSiteUseCase
import org.wordpress.android.util.UrlUtilsWrapper

private const val SITE_TITLE = "site title"
private val DUMMY_SITE_DATA: SiteCreationServiceData = SiteCreationServiceData(
        123,
        "slug",
        "domain",
        SITE_TITLE
)
private const val LANGUAGE_ID = "lang_id"
private const val TIMEZONE_ID = "timezone_id"

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CreateSiteUseCaseTest : BaseUnitTest() {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var store: SiteStore
    @Mock private lateinit var urlUtilsWrapper: UrlUtilsWrapper
    private lateinit var useCase: CreateSiteUseCase
    private lateinit var event: OnNewSiteCreated

    @Before
    fun setUp() {
        useCase = CreateSiteUseCase(dispatcher, store, urlUtilsWrapper)
        event = OnNewSiteCreated(newSiteRemoteId = 123)
    }

    @Test
    fun coroutineResumedWhenResultEventDispatched() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onNewSiteCreated(event) }
        val resultEvent = useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID)

        assertThat(resultEvent).isEqualTo(event)
    }

    @Test
    fun verifySiteDataPropagated() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onNewSiteCreated(event) }
        useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID)

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(captor.capture())

        assertThat(captor.value.type).isEqualTo(SiteAction.CREATE_NEW_SITE)
        assertThat(captor.value.payload).isInstanceOf(NewSitePayload::class.java)
        val payload = captor.value.payload as NewSitePayload
        assertThat(payload.siteName).isEqualTo(DUMMY_SITE_DATA.domain)
        assertThat(payload.segmentId).isEqualTo(DUMMY_SITE_DATA.segmentId)
        assertThat(payload.siteTitle).isEqualTo(SITE_TITLE)
    }

    @Test
    fun verifyDryRunIsFalse() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onNewSiteCreated(event) }
        useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID)

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(captor.capture())

        val payload = captor.value.payload as NewSitePayload
        assertThat(payload.dryRun).isEqualTo(false)
    }

    @Test
    fun verifyCreatesPublicSite() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onNewSiteCreated(event) }
        useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID)

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(captor.capture())

        val payload = captor.value.payload as NewSitePayload
        assertThat(payload.visibility).isEqualTo(SiteVisibility.PUBLIC)
    }

    @Test
    fun verifyPropagatesLanguageId() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onNewSiteCreated(event) }
        useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID)

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(captor.capture())

        val payload = captor.value.payload as NewSitePayload
        assertThat(payload.language).isEqualTo(LANGUAGE_ID)
    }

    @Test
    fun verifyPropagatesTimeZoneId() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onNewSiteCreated(event) }
        useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID)

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(captor.capture())

        val payload = captor.value.payload as NewSitePayload
        assertThat(payload.timeZoneId).isEqualTo(TIMEZONE_ID)
    }
}
