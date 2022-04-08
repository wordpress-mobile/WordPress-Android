package org.wordpress.android.ui.sitecreation.previews

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility
import org.wordpress.android.test
import org.wordpress.android.ui.accounts.signup.SignupUtils
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
private const val USERNAME = "username"
private const val EMAIL_USERNAME = "emailusername"
private const val EMAIL = EMAIL_USERNAME + "@domain.tl"
private const val LANGUAGE_ID = "lang_id"
private const val TIMEZONE_ID = "timezone_id"

@RunWith(MockitoJUnitRunner::class)
class CreateSiteUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var store: SiteStore
    @Mock private lateinit var urlUtilsWrapper: UrlUtilsWrapper
    @Mock private lateinit var accountStore: AccountStore
    @Mock lateinit var accountModel: AccountModel
    private lateinit var useCase: CreateSiteUseCase
    private lateinit var event: OnNewSiteCreated
    private val signupUtils = SignupUtils()

    @Before
    fun setUp() {
        whenever(accountStore.account).thenReturn(accountModel)
        whenever(accountModel.userName).thenReturn(USERNAME)
        whenever(accountModel.email).thenReturn(EMAIL)
        useCase = CreateSiteUseCase(dispatcher, store, urlUtilsWrapper, accountStore, signupUtils)
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
        assertThat(payload.username).isEqualTo(USERNAME)
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

    @Test
    fun verifyPropagatesEmailUsernameWhenUsernameIsNull() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onNewSiteCreated(event) }
        whenever(accountModel.userName).thenReturn(null)
        useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID)

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(captor.capture())

        val payload = captor.value.payload as NewSitePayload
        assertThat(payload.username).isEqualTo(EMAIL_USERNAME)
    }
}
