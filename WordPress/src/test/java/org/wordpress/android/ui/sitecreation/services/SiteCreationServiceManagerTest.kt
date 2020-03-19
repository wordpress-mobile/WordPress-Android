package org.wordpress.android.ui.sitecreation.services

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.SiteStore.NewSiteError
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType.SITE_NAME_EXISTS
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceManager.SiteCreationServiceManagerListener
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.SUCCESS
import org.wordpress.android.ui.sitecreation.usecases.CreateSiteUseCase

private const val LANGUAGE_ID = "lang_id"
private const val NEW_SITE_REMOTE_ID = 1234L

private val DUMMY_SITE_DATA: SiteCreationServiceData = SiteCreationServiceData(
        123,
        "slug"
)

private val IDLE_STATE = SiteCreationServiceState(IDLE)
private val CREATE_SITE_STATE = SiteCreationServiceState(CREATE_SITE)
private val SUCCESS_STATE = SiteCreationServiceState(SUCCESS, NEW_SITE_REMOTE_ID)
private val FAILURE_STATE = SiteCreationServiceState(FAILURE)

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteCreationServiceManagerTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var useCase: CreateSiteUseCase
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var serviceListener: SiteCreationServiceManagerListener
    @Mock lateinit var tracker: SiteCreationTracker

    private lateinit var manager: SiteCreationServiceManager

    private val successEvent = OnNewSiteCreated()
    private val genericErrorEvent = OnNewSiteCreated()
    private val siteExistsErrorEvent = OnNewSiteCreated()

    @Before
    fun setUp() {
        manager = SiteCreationServiceManager(useCase, dispatcher, tracker, TEST_DISPATCHER)
        successEvent.newSiteRemoteId = NEW_SITE_REMOTE_ID
        siteExistsErrorEvent.newSiteRemoteId = NEW_SITE_REMOTE_ID
        genericErrorEvent.error = NewSiteError(GENERIC_ERROR, "")
        siteExistsErrorEvent.error = NewSiteError(SITE_NAME_EXISTS, "")
    }

    @Test
    fun verifyServiceStateIsBeingUpdated() = test {
        setSuccessfulResponses()
        startFlow()

        argumentCaptor<SiteCreationServiceState>().apply {
            verify(serviceListener, times(3)).updateState(capture())
            assertThat(allValues[0]).isEqualTo(IDLE_STATE)
            assertThat(allValues[1]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[2]).isEqualTo(SUCCESS_STATE)
        }
    }

    @Test
    fun verifyServiceStateUpdateToFailureOnError() = test {
        setGenericErrorResponses()
        startFlow()

        argumentCaptor<SiteCreationServiceState>().apply {
            verify(serviceListener, times(3)).updateState(capture())
            assertThat(allValues[0]).isEqualTo(IDLE_STATE)
            assertThat(allValues[1]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[2]).isEqualTo(SiteCreationServiceState(FAILURE))
        }
    }

    @Test
    fun verifyServicePropagatesCurrentStateWhenFails() = test {
        setGenericErrorResponses()
        val stateBeforeFailure = CREATE_SITE_STATE
        whenever(serviceListener.getCurrentState()).thenReturn(stateBeforeFailure)

        startFlow()

        argumentCaptor<SiteCreationServiceState>().apply {
            verify(serviceListener, times(3)).updateState(capture())
            assertThat(allValues[0]).isEqualTo(IDLE_STATE)
            assertThat(allValues[1]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[2]).isEqualTo(FAILURE_STATE.copy(payload = stateBeforeFailure))
        }
    }

    @Test
    fun verifyRetryWorksWhenTheSiteWasCreatedInPreviousAttempt() = test {
        setSiteExistsErrorResponses()
        retryFlow(previousState = CREATE_SITE_STATE.stepName)

        val argumentCaptor = argumentCaptor<SiteCreationServiceState>()
        argumentCaptor.apply {
            verify(serviceListener, times(3)).updateState(capture())
            assertThat(allValues[0]).isEqualTo(IDLE_STATE)
            assertThat(allValues[1]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[2]).isEqualTo(SUCCESS_STATE)
        }
    }

    @Test
    fun verifyRetryWorksWhenCreateSiteRequestFailed() = test {
        setGenericErrorResponses()
        startFlow()

        setSuccessfulResponses()
        retryFlow(previousState = CREATE_SITE_STATE.stepName)

        val argumentCaptor = argumentCaptor<SiteCreationServiceState>()
        argumentCaptor.apply {
            verify(serviceListener, times(6)).updateState(capture())
            assertThat(allValues[0]).isEqualTo(IDLE_STATE)
            assertThat(allValues[1]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[2]).isEqualTo(FAILURE_STATE)

            assertThat(allValues[3]).isEqualTo(IDLE_STATE)
            assertThat(allValues[4]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[5]).isEqualTo(SUCCESS_STATE)
        }
    }

    @Test
    fun verifyUseCaseRegisteredToDispatcherOnCreate() {
        manager.onCreate()
        argumentCaptor<CreateSiteUseCase>().apply {
            verify(dispatcher).register(capture())
            assertThat(allValues[0]).isEqualTo(useCase)
        }
    }

    @Test
    fun verifyUseCaseUnregisteredFromDispatcherOnDestroy() {
        manager.onDestroy()
        argumentCaptor<CreateSiteUseCase>().apply {
            verify(dispatcher).unregister(capture())
            assertThat(allValues[0]).isEqualTo(useCase)
        }
    }

    @Test
    fun verifyDispatcherRegistrationHandledCorrectly() = test {
        setGenericErrorResponses()
        manager.onCreate()
        startFlow()
        setSuccessfulResponses()
        retryFlow(previousState = CREATE_SITE_STATE.stepName)
        manager.onDestroy()
        argumentCaptor<CreateSiteUseCase>().apply {
            verify(dispatcher).register(capture())
            assertThat(allValues[0]).isEqualTo(useCase)
        }
        argumentCaptor<CreateSiteUseCase>().apply {
            verify(dispatcher).unregister(capture())
            assertThat(allValues[0]).isEqualTo(useCase)
        }
    }

    @Test
    fun verifyIllegalStateExceptionInUseCaseResultsInServiceErrorState() = test {
        whenever(useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID))
                .thenThrow(IllegalStateException("Error"))
        startFlow()
        argumentCaptor<SiteCreationServiceState>().apply {
            verify(serviceListener, times(3)).updateState(capture())
            assertThat(allValues[0]).isEqualTo(IDLE_STATE)
            assertThat(allValues[1]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[2]).isEqualTo(SiteCreationServiceState(FAILURE))
        }
    }

    private fun startFlow() {
        manager.onStart(LANGUAGE_ID, null, DUMMY_SITE_DATA, serviceListener)
    }

    private fun retryFlow(previousState: String) {
        manager.onStart(LANGUAGE_ID, previousState, DUMMY_SITE_DATA, serviceListener)
    }

    private suspend fun setSuccessfulResponses() = test {
        whenever(useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID))
                .thenReturn(successEvent)
    }

    private suspend fun setGenericErrorResponses() = test {
        whenever(useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID))
                .thenReturn(genericErrorEvent)
    }

    private suspend fun setSiteExistsErrorResponses() = test {
        whenever(useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID))
                .thenReturn(siteExistsErrorEvent)
    }
}
