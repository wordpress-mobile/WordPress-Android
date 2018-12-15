package org.wordpress.android.ui.sitecreation.creation

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
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
import org.wordpress.android.ui.sitecreation.NewSiteCreationTracker
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceManager.NewSiteCreationServiceManagerListener
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.SUCCESS

private const val LANGUAGE_ID = "lang_id"
private const val NEW_SITE_REMOTE_ID = 1234L

private val DUMMY_SITE_DATA: NewSiteCreationServiceData = NewSiteCreationServiceData(
        123,
        999,
        "title",
        "tagLine",
        "slug"
)
private val SUCCESS_EVENT = OnNewSiteCreated()
private val GENERIC_ERROR_EVENT = OnNewSiteCreated()
private val SITE_EXISTS_ERROR_EVENT = OnNewSiteCreated()

private val IDLE_STATE = NewSiteCreationServiceState(IDLE)
private val CREATE_SITE_STATE = NewSiteCreationServiceState(CREATE_SITE)
private val SUCCESS_STATE = NewSiteCreationServiceState(SUCCESS, NEW_SITE_REMOTE_ID)
private val FAILURE_STATE = NewSiteCreationServiceState(FAILURE)

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationServiceManagerTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var useCase: CreateSiteUseCase
    @Mock lateinit var tracker: NewSiteCreationTracker
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var serviceListener: NewSiteCreationServiceManagerListener

    private lateinit var manager: NewSiteCreationServiceManager

    @Before
    fun setUp() {
        manager = NewSiteCreationServiceManager(useCase, dispatcher, tracker, TEST_DISPATCHER)
        SUCCESS_EVENT.newSiteRemoteId = NEW_SITE_REMOTE_ID
        SITE_EXISTS_ERROR_EVENT.newSiteRemoteId = NEW_SITE_REMOTE_ID
        GENERIC_ERROR_EVENT.error = NewSiteError(GENERIC_ERROR, "")
        SITE_EXISTS_ERROR_EVENT.error = NewSiteError(SITE_NAME_EXISTS, "")
    }

    @Test
    fun verifyServiceStateIsBeingUpdated() = test {
        setSuccessfulResponses()
        startFlow()

        argumentCaptor<NewSiteCreationServiceState>().apply {
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

        argumentCaptor<NewSiteCreationServiceState>().apply {
            verify(serviceListener, times(3)).updateState(capture())
            assertThat(allValues[0]).isEqualTo(IDLE_STATE)
            assertThat(allValues[1]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[2]).isEqualTo(NewSiteCreationServiceState(FAILURE))
        }
    }

    @Test
    fun verifyServicePropagatesCurrentStateWhenFails() = test {
        setGenericErrorResponses()
        val stateBeforeFailure = CREATE_SITE_STATE
        whenever(serviceListener.getCurrentState()).thenReturn(stateBeforeFailure)

        startFlow()

        argumentCaptor<NewSiteCreationServiceState>().apply {
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

        val argumentCaptor = argumentCaptor<NewSiteCreationServiceState>()
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

        val argumentCaptor = argumentCaptor<NewSiteCreationServiceState>()
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

    private fun startFlow() {
        manager.onStart(LANGUAGE_ID, null, DUMMY_SITE_DATA, serviceListener)
    }

    private fun retryFlow(previousState: String) {
        manager.onStart(LANGUAGE_ID, previousState, DUMMY_SITE_DATA, serviceListener)
    }

    private suspend fun setSuccessfulResponses() = test {
        whenever(useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID))
                .thenReturn(SUCCESS_EVENT)
    }

    private suspend fun setGenericErrorResponses() = test {
        whenever(useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID))
                .thenReturn(GENERIC_ERROR_EVENT)
    }

    private suspend fun setSiteExistsErrorResponses() = test {
        whenever(useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID))
                .thenReturn(SITE_EXISTS_ERROR_EVENT)
    }
}
