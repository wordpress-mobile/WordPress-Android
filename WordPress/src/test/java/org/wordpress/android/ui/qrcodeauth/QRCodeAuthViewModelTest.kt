package org.wordpress.android.ui.qrcodeauth

import android.os.Bundle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.times
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QRLOGIN_VERIFY_FAILED
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthError
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.NOT_AUTHORIZED
import org.wordpress.android.fluxc.store.qrcodeauth.QRCodeAuthStore
import org.wordpress.android.fluxc.store.qrcodeauth.QRCodeAuthStore.QRCodeAuthAuthenticateResult
import org.wordpress.android.fluxc.store.qrcodeauth.QRCodeAuthStore.QRCodeAuthResult
import org.wordpress.android.fluxc.store.qrcodeauth.QRCodeAuthStore.QRCodeAuthValidateResult
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Content.Done
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Content.Validated
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Error.InvalidData
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Error.NoInternet
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.AUTHENTICATING
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.AUTHENTICATION_FAILED
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.DONE
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.EXPIRED_TOKEN
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.INVALID_DATA
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.NO_INTERNET
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.VALIDATED
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthViewModel.Companion.BROWSER_KEY
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthViewModel.Companion.DATA_KEY
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthViewModel.Companion.LAST_STATE_KEY
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthViewModel.Companion.LOCATION_KEY
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthViewModel.Companion.ORIGIN_DEEPLINK
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthViewModel.Companion.ORIGIN_MENU
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthViewModel.Companion.TOKEN_KEY
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthViewModel.Companion.TRACKING_ORIGIN_KEY
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

const val DATA = "data"
const val LOCATION = "location"
const val BROWSER = "browser"
const val TOKEN = "token"
const val SCANNED_VALUE =
        "https://apps.wordpress.com/get/?campaign=login-qr-code#qr-code-login?token=scannedtoken&data=scanneddata"
const val VALID_EXPIRED_MESSAGE = "qr code data expired"
const val INVALID_EXPIRED_MESSAGE = "invalid qr code data expired"

@Suppress("LargeClass")
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class QRCodeAuthViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: QRCodeAuthViewModel
    @Mock lateinit var store: QRCodeAuthStore
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var savedInstanceState: Bundle
    @Mock lateinit var validator: QRCodeAuthValidator
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    private val uiStateMapper = QRCodeAuthUiStateMapper()

    private val validQueryParams = mapOf(DATA_KEY to DATA, TOKEN_KEY to TOKEN)
    private val invalidQueryParams = mapOf("invalid_key" to DATA, TOKEN_KEY to TOKEN)

    private val errorTrackingMapInvalidData = mutableMapOf("error" to "invalid_data", "origin" to "menu")
    private val errorTrackingMapAuthFailed = mutableMapOf("error" to "authentication_failed", "origin" to "menu")
    private val errorTrackingMapExpiredToken = mutableMapOf("error" to "expired_token", "origin" to "menu")

    @Before
    fun setUp() {
        viewModel = QRCodeAuthViewModel(
                store,
                uiStateMapper,
                networkUtilsWrapper,
                validator,
                analyticsTrackerWrapper
        )

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(validator.isValidUri(SCANNED_VALUE)).thenReturn(true)
        whenever(validator.extractQueryParams(SCANNED_VALUE)).thenReturn(validQueryParams)
    }

    @Test
    fun `given empty instance state, when vm started, then loading is followed by scanning`() = runBlockingTest {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            viewModel.start()

            assertThat(uiStates.last()).isInstanceOf(QRCodeAuthUiState.Scanning::class.java)
        }
    }

    @Test
    fun `given non empty instance state, when vm started, then state is restored`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initAndStartVMForState(NO_INTERNET)

            assertThat(uiStates.last().type).isEqualTo(NO_INTERNET)
        }
    }

    @Test
    fun `given onSavedInstanceState, when writeToBundle is invoked, then properties are written To Bundle`() {
        startViewModel()

        viewModel.writeToBundle(savedInstanceState)

        verify(savedInstanceState, times(2)).putString(any(), argThat { true })
    }

    @Test
    fun `given validate state, when primary action is clicked, then state is authenticating`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initAndStartVMForState(VALIDATED)

            (uiStates.last() as Validated).primaryActionButton.clickAction()

            assertThat(uiStates.last().type).isEqualTo(AUTHENTICATING)
        }
    }

    @Test
    fun `given validate state, when secondary action is clicked, then activity finished event`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        runBlockingTestWithData(uiStates, actionEvents) {
            initAndStartVMForState(VALIDATED)

            (uiStates.last() as Validated).secondaryActionButton.clickAction()

            assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.FinishActivity::class.java)
        }
    }

    @Test
    fun `given valid qr code, when scanned qrcode, then validated is shown`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initValidate()

            viewModel.start()
            viewModel.onScanSuccess(SCANNED_VALUE)

            assertThat(uiStates.last().type).isEqualTo(VALIDATED)
        }
    }

    @Test
    fun `given invalid host, when scanned qrcode, then error is shown`() {
        whenever(validator.isValidUri(SCANNED_VALUE)).thenReturn(false)

        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            viewModel.start()
            viewModel.onScanSuccess(SCANNED_VALUE)

            assertThat(uiStates.last().type).isEqualTo(INVALID_DATA)
        }
    }

    @Test
    fun `given invalid host, when scanned qrcode, then invalid data error is tracked`() {
        whenever(validator.isValidUri(SCANNED_VALUE)).thenReturn(false)

        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            viewModel.start()
            viewModel.onScanSuccess(SCANNED_VALUE)

            verify(analyticsTrackerWrapper).track(eq(QRLOGIN_VERIFY_FAILED), eq(errorTrackingMapInvalidData))
        }
    }

    @Test
    fun `given invalid query params, when scanned qrcode, then error is shown`() {
        whenever(validator.isValidUri(SCANNED_VALUE)).thenReturn(true)
        whenever(validator.extractQueryParams(SCANNED_VALUE)).thenReturn(invalidQueryParams)

        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            viewModel.start()
            viewModel.onScanSuccess(SCANNED_VALUE)

            assertThat(uiStates.last().type).isEqualTo(INVALID_DATA)
        }
    }

    @Test
    fun `given invalid query params, when scanned qrcode, then invalid data error is tracked`() {
        whenever(validator.isValidUri(SCANNED_VALUE)).thenReturn(true)
        whenever(validator.extractQueryParams(SCANNED_VALUE)).thenReturn(invalidQueryParams)

        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            viewModel.start()
            viewModel.onScanSuccess(SCANNED_VALUE)

            verify(analyticsTrackerWrapper).track(eq(QRLOGIN_VERIFY_FAILED), eq(errorTrackingMapInvalidData))
        }
    }

    @Test
    fun `given not authorized error, when validate failure, then auth failed is shown`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initValidate(false, NOT_AUTHORIZED)

            viewModel.start()
            viewModel.onScanSuccess(SCANNED_VALUE)

            assertThat(uiStates.last().type).isEqualTo(AUTHENTICATION_FAILED)
        }
    }

    @Test
    fun `given not authorized error, when validate failure, then auth failed error is tracked`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initValidate(false, NOT_AUTHORIZED)

            viewModel.start()
            viewModel.onScanSuccess(SCANNED_VALUE)

            verify(analyticsTrackerWrapper).track(eq(QRLOGIN_VERIFY_FAILED), eq(errorTrackingMapAuthFailed))
        }
    }

    @Test
    fun `given error, when validate failure, then invalid data is shown`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initValidate(false, GENERIC_ERROR)

            viewModel.start()
            viewModel.onScanSuccess(SCANNED_VALUE)

            assertThat(uiStates.last().type).isEqualTo(INVALID_DATA)
        }
    }

    @Test
    fun `given error, when validate failure, then invalid data is tracked`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initValidate(false, GENERIC_ERROR)

            viewModel.start()
            viewModel.onScanSuccess(SCANNED_VALUE)

            verify(analyticsTrackerWrapper).track(eq(QRLOGIN_VERIFY_FAILED), eq(errorTrackingMapInvalidData))
        }
    }

    @Test
    fun `given authorization required with valid error message, when validate failure, then expired is shown`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initValidate(false, AUTHORIZATION_REQUIRED, VALID_EXPIRED_MESSAGE)

            viewModel.start()
            viewModel.onScanSuccess(SCANNED_VALUE)

            assertThat(uiStates.last().type).isEqualTo(EXPIRED_TOKEN)
        }
    }

    @Test
    fun `given authorization required with valid error message, when validate failure, then expired is tracked`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initValidate(false, AUTHORIZATION_REQUIRED, VALID_EXPIRED_MESSAGE)

            viewModel.start()
            viewModel.onScanSuccess(SCANNED_VALUE)

            verify(analyticsTrackerWrapper).track(eq(QRLOGIN_VERIFY_FAILED), eq(errorTrackingMapExpiredToken))
        }
    }

    @Test
    fun `given authorization required with invalid error message, when validate failure, then auth failed is shown`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initValidate(false, AUTHORIZATION_REQUIRED, INVALID_EXPIRED_MESSAGE)

            viewModel.start()
            viewModel.onScanSuccess(SCANNED_VALUE)

            assertThat(uiStates.last().type).isEqualTo(AUTHENTICATION_FAILED)
        }
    }

    @Test
    fun `given authorization required with invalid error message, when validate failure, then authfailed is tracked`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initValidate(false, AUTHORIZATION_REQUIRED, INVALID_EXPIRED_MESSAGE)

            viewModel.start()
            viewModel.onScanSuccess(SCANNED_VALUE)

            verify(analyticsTrackerWrapper).track(eq(QRLOGIN_VERIFY_FAILED), eq(errorTrackingMapAuthFailed))
        }
    }

    @Test
    fun `given validated state, when authenticate invoked, then authenticating followed by done`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initAuthenticate()
            initAndStartVMForState(VALIDATED)

            val initialState = uiStates.last()
            uiStates.clear()
            (initialState as Validated).primaryActionButton.clickAction()

            assertThat(uiStates.first().type).isEqualTo(AUTHENTICATING)
            assertThat(uiStates.last().type).isEqualTo(DONE)
        }
    }

    @Test
    fun `given not authorized error, when authenticate failure, then auth failed is shown`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initAuthenticate(false, NOT_AUTHORIZED)
            initAndStartVMForState(VALIDATED)

            (uiStates.last() as Validated).primaryActionButton.clickAction()

            assertThat(uiStates.last().type).isEqualTo(AUTHENTICATION_FAILED)
        }
    }

    @Test
    fun `given not authorized error, when authenticate failure, then auth failed is tracked`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initAuthenticate(false, NOT_AUTHORIZED)
            initAndStartVMForState(VALIDATED)

            (uiStates.last() as Validated).primaryActionButton.clickAction()

            verify(analyticsTrackerWrapper).track(eq(QRLOGIN_VERIFY_FAILED), eq(errorTrackingMapAuthFailed))
        }
    }

    @Test
    fun `given error, when authenticate failure, then invalid data is shown`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initAuthenticate(false, GENERIC_ERROR)
            initAndStartVMForState(VALIDATED)

            (uiStates.last() as Validated).primaryActionButton.clickAction()

            assertThat(uiStates.last().type).isEqualTo(INVALID_DATA)
        }
    }

    @Test
    fun `given error, when authenticate failure, then invalid data is tracked`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initAuthenticate(false, GENERIC_ERROR)
            initAndStartVMForState(VALIDATED)

            (uiStates.last() as Validated).primaryActionButton.clickAction()

            verify(analyticsTrackerWrapper).track(eq(Stat.QRLOGIN_VERIFY_APPROVED), any<MutableMap<String, Any>>())
            verify(analyticsTrackerWrapper).track(eq(QRLOGIN_VERIFY_FAILED), eq(errorTrackingMapInvalidData))
        }
    }

    @Test
    fun `given authorization required with valid message, when authenticate failure, then expired is shown`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initAuthenticate(false, AUTHORIZATION_REQUIRED, VALID_EXPIRED_MESSAGE)
            initAndStartVMForState(VALIDATED)

            (uiStates.last() as Validated).primaryActionButton.clickAction()

            assertThat(uiStates.last().type).isEqualTo(EXPIRED_TOKEN)
        }
    }

    @Test
    fun `given authorization required with valid message, when authenticate failure, then expired is tracked`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initAuthenticate(false, AUTHORIZATION_REQUIRED, VALID_EXPIRED_MESSAGE)
            initAndStartVMForState(VALIDATED)

            (uiStates.last() as Validated).primaryActionButton.clickAction()

            verify(analyticsTrackerWrapper).track(eq(Stat.QRLOGIN_VERIFY_APPROVED), any<MutableMap<String, Any>>())
            verify(analyticsTrackerWrapper).track(eq(QRLOGIN_VERIFY_FAILED), eq(errorTrackingMapExpiredToken))
        }
    }

    @Test
    fun `given authorization required with invalid message, when authenticate failure, then auth failed is shown`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initAuthenticate(false, AUTHORIZATION_REQUIRED, INVALID_EXPIRED_MESSAGE)
            initAndStartVMForState(VALIDATED)

            (uiStates.last() as Validated).primaryActionButton.clickAction()

            assertThat(uiStates.last().type).isEqualTo(AUTHENTICATION_FAILED)
        }
    }

    @Test
    fun `given authorization required with invalid message, when authenticate failure, then auth failed is tracked`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            initAuthenticate(false, AUTHORIZATION_REQUIRED, INVALID_EXPIRED_MESSAGE)
            initAndStartVMForState(VALIDATED)

            (uiStates.last() as Validated).primaryActionButton.clickAction()

            verify(analyticsTrackerWrapper).track(eq(Stat.QRLOGIN_VERIFY_APPROVED), any<MutableMap<String, Any>>())
            verify(analyticsTrackerWrapper).track(eq(QRLOGIN_VERIFY_FAILED), eq(errorTrackingMapAuthFailed))
        }
    }

    @Test
    fun `given done, when primary action is clicked, then finish activity is raised`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        runBlockingTestWithData(uiStates, actionEvents) {
            initAndStartVMForState(DONE)

            (uiStates.last() as Done).primaryActionButton.clickAction()

            assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.FinishActivity::class.java)
        }
    }

    @Test
    fun `given error, when primary action clicked, then launch scanner is raised`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        runBlockingTestWithData(uiStates, actionEvents) {
            initAndStartVMForState(INVALID_DATA)

            (uiStates.last() as InvalidData).primaryActionButton.clickAction()

            assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.LaunchScanner::class.java)
        }
    }

    @Test
    fun `given error, when secondary action clicked, then finish activity is raised`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        runBlockingTestWithData(uiStates, actionEvents) {
            initAndStartVMForState(INVALID_DATA)

            (uiStates.last() as InvalidData).secondaryActionButton.clickAction()

            assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.FinishActivity::class.java)
        }
    }

    @Test
    fun `given any state, when back is pressed, then dismiss dialog event is raised`() {
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        runBlockingTestWithData(actionEvents = actionEvents) {
            viewModel.onBackPressed()

            assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.LaunchDismissDialog::class.java)
        }
    }

    @Test
    fun `when scan fails, then finish activity event is raised`() {
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        runBlockingTestWithData(actionEvents = actionEvents) {
            viewModel.onScanFailure()

            assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.FinishActivity::class.java)
        }
    }

    @Test
    fun `given valid scan, when no network connection, then no internet error is shown`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
            startViewModel()
            viewModel.onScanSuccess(SCANNED_VALUE)

            assertThat(uiStates.last()).isInstanceOf(NoInternet::class.java)
        }
    }

    @Test
    fun `given authenticating, when no network connection, then error view is shown`() {
        val uiStates = mutableListOf<QRCodeAuthUiState>()
        runBlockingTestWithData(uiStates) {
            whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
            initAndStartVMForState(VALIDATED)

            (uiStates.last() as Validated).primaryActionButton.clickAction()

            assertThat(uiStates.last().type).isEqualTo(NO_INTERNET)
        }
    }

    @Test
    fun `given dismiss dialog showing, when ok clicked, then finish activity event is raised`() {
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        runBlockingTestWithData(actionEvents = actionEvents) {
            viewModel.onDialogInteraction(DialogInteraction.Positive("positive"))

            assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.FinishActivity::class.java)
        }
    }

    @Test
    fun `given dismiss dialog showing, when cancel clicked, then no event is raised`() {
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        runBlockingTestWithData(actionEvents = actionEvents) {
            viewModel.onDialogInteraction(DialogInteraction.Negative("negative"))

            assertThat(actionEvents.isEmpty())
        }
    }

    private fun buildValidateError(errorType: QRCodeAuthErrorType, errorMessage: String? = null) =
            QRCodeAuthResult<QRCodeAuthValidateResult>(QRCodeAuthError(errorType, errorMessage))

    private fun buildValidateSuccess() =
            QRCodeAuthResult(model = QRCodeAuthValidateResult(browser = BROWSER, location = LOCATION))

    private fun buildAuthenticateError(errorType: QRCodeAuthErrorType, errorMessage: String? = null) =
            QRCodeAuthResult<QRCodeAuthAuthenticateResult>(QRCodeAuthError(errorType, errorMessage))

    private val authenticateSuccess = QRCodeAuthResult(model = QRCodeAuthAuthenticateResult(authenticated = true))

    private fun startViewModel(saveInstanceState: Bundle? = null) {
        viewModel.start(savedInstanceState = saveInstanceState)
    }

    private fun initAndStartVMForState(stateType: QRCodeAuthUiStateType) {
        initSavedInstanceState(stateType)
        viewModel.start(savedInstanceState = savedInstanceState)
    }

    private fun initSavedInstanceState(stateType: QRCodeAuthUiStateType, isDeepLink: Boolean = false) {
        whenever(savedInstanceState.getString(DATA_KEY, null)).thenReturn(DATA)
        whenever(savedInstanceState.getString(TOKEN_KEY, null)).thenReturn(TOKEN)
        whenever(savedInstanceState.getString(LOCATION_KEY, null)).thenReturn(LOCATION)
        whenever(savedInstanceState.getString(BROWSER_KEY, null)).thenReturn(BROWSER)
        whenever(savedInstanceState.getString(LAST_STATE_KEY, null)).thenReturn(stateType.label)
        if (isDeepLink) {
            whenever(savedInstanceState.getString(TRACKING_ORIGIN_KEY, ORIGIN_DEEPLINK)).thenReturn(ORIGIN_DEEPLINK)
        } else {
            whenever(savedInstanceState.getString(TRACKING_ORIGIN_KEY, ORIGIN_MENU)).thenReturn(ORIGIN_MENU)
        }
    }

    private suspend fun initValidate(
        successResponse: Boolean = true,
        errorType: QRCodeAuthErrorType? = INVALID_RESPONSE,
        errorMessage: String? = null
    ) {
        whenever(store.validate(any(), any())).thenReturn(
                if (successResponse) {
                    buildValidateSuccess()
                } else {
                    buildValidateError(errorType as QRCodeAuthErrorType, errorMessage)
                }
        )
    }

    private suspend fun initAuthenticate(
        successResponse: Boolean = true,
        errorType: QRCodeAuthErrorType? = INVALID_RESPONSE,
        errorMessage: String? = null
    ) {
        whenever(store.authenticate(any(), any())).thenReturn(
                if (successResponse) {
                    authenticateSuccess
                } else {
                    buildAuthenticateError(errorType as QRCodeAuthErrorType, errorMessage)
                }
        )
    }

    private fun runBlockingTestWithData(
        uiStates: MutableList<QRCodeAuthUiState> = mutableListOf(),
        actionEvents: MutableList<QRCodeAuthActionEvent> = mutableListOf(),
        testBody: suspend TestCoroutineScope.() -> Unit
    ) {
        runBlockingTest {
            val uiStatesJob = launch { viewModel.uiState.toList(uiStates) }
            val actionEventsJob = launch { viewModel.actionEvents.toList(actionEvents) }
            testBody()
            uiStatesJob.cancel()
            actionEventsJob.cancel()
        }
    }
}
