package org.wordpress.android.ui.qrcodeauth

import android.os.Bundle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthError
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.DATA_INVALID
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
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.AUTH_FAILED
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.DONE
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.EXPIRED
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.INVALID_DATA
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.NO_INTERNET
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.VALIDATED
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthViewModel.Companion.BROWSER_KEY
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthViewModel.Companion.DATA_KEY
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthViewModel.Companion.LAST_STATE_KEY
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthViewModel.Companion.LOCATION_KEY
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthViewModel.Companion.TOKEN_KEY
import org.wordpress.android.util.NetworkUtilsWrapper

const val DATA = "data"
const val LOCATION = "location"
const val BROWSER = "browser"
const val TOKEN = "token"
const val SCANNED_VALUE =
        "https://apps.wordpress.com/get/?campaign=login-qr-code#qr-code-login?token=scannedtoken&data=scanneddata"

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@Suppress("LargeClass")
class QRCodeAuthViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: QRCodeAuthViewModel
    @Mock lateinit var store: QRCodeAuthStore
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var savedInstanceState: Bundle
    @Mock lateinit var validator: QRCodeAuthValidator
    private val uiStateMapper = QRCodeAuthUiStateMapper()

    private val validQueryParams = mapOf(DATA_KEY to DATA, TOKEN_KEY to TOKEN)
    private val invalidQueryParams = mapOf("invalid_key" to DATA, TOKEN_KEY to TOKEN)
    @Before
    fun setUp() {
        viewModel = QRCodeAuthViewModel(
                store,
                uiStateMapper,
                networkUtilsWrapper,
                validator
        )

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(validator.isValidUri(SCANNED_VALUE)).thenReturn(true)
        whenever(validator.extractQueryParams(SCANNED_VALUE)).thenReturn(validQueryParams)
    }

    @Test
    fun `given empty instance state, when vm started, then loading is followed by scanning`() = runBlockingTest {
        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        viewModel.start()

        assert(result.first().loadingVisibility)
        assertThat(result.last()).isInstanceOf(QRCodeAuthUiState.Scanning::class.java)

        job.cancel()
    }

    @Test
    fun `given non empty instance state, when vm started, then state is restored`() = runBlockingTest {
        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        initAndStartVMForState(NO_INTERNET)

        assert(result.first().loadingVisibility)
        assertThat(result.last().type).isEqualTo(NO_INTERNET)

        job.cancel()
    }

    @Test
    fun `given onSavedInstanceState, when writeToBundle is invoked, then properties are written To Bundle`() {
        startViewModel()

        viewModel.writeToBundle(savedInstanceState)

        verify(savedInstanceState).putString(any(), argThat { true })
    }

    @Test
    fun `given validate state, when primary action is clicked, then state is authenticating`() = runBlockingTest {
        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        initAndStartVMForState(VALIDATED)

        (result.last() as Validated).primaryActionButton.clickAction()

        assertThat(result.last().type).isEqualTo(AUTHENTICATING)

        job.cancel()
    }

    @Test
    fun `given validate state, when secondary action is clicked, then activity finished event`() = runBlockingTest {
        val result = mutableListOf<QRCodeAuthUiState>()
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        val job1 = launch {
            viewModel.uiState.toList(result)
        }
        val job2 = launch {
            viewModel.actionEvents.collect {
                actionEvents.add(it)
            }
        }

        initAndStartVMForState(VALIDATED)

        (result.last() as Validated).secondaryActionButton.clickAction()

        assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.FinishActivity::class.java)

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun `given valid qr code, when scanned qrcode, then validated is shown`() = runBlockingTest {
        initValidate()

        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        viewModel.start()
        viewModel.onScanSuccess(SCANNED_VALUE)

        assertThat(result.last().type).isEqualTo(VALIDATED)

        job.cancel()
    }

    @Test
    fun `given invalid host, when scanned qrcode, then error is shown`() = runBlockingTest {
        whenever(validator.isValidUri(SCANNED_VALUE)).thenReturn(false)

        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        viewModel.start()
        viewModel.onScanSuccess(SCANNED_VALUE)

        assertThat(result.last().type).isEqualTo(INVALID_DATA)

        job.cancel()
    }

    @Test
    fun `given invalid query params, when scanned qrcode, then error is shown`() = runBlockingTest {
        whenever(validator.isValidUri(SCANNED_VALUE)).thenReturn(true)
        whenever(validator.extractQueryParams(SCANNED_VALUE)).thenReturn(invalidQueryParams)

        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        viewModel.start()
        viewModel.onScanSuccess(SCANNED_VALUE)

        assertThat(result.last().type).isEqualTo(INVALID_DATA)

        job.cancel()
    }

    @Test
    fun `given not authorized error, when validate failure, then auth failed is shown`() = runBlockingTest {
        initValidate(false, NOT_AUTHORIZED)

        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        viewModel.start()
        viewModel.onScanSuccess(SCANNED_VALUE)

        assertThat(result.last().type).isEqualTo(AUTH_FAILED)

        job.cancel()
    }

    @Test
    fun `given error, when validate failure, then invalid data is shown`() = runBlockingTest {
        initValidate(false, GENERIC_ERROR)

        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        viewModel.start()
        viewModel.onScanSuccess(SCANNED_VALUE)

        assertThat(result.last().type).isEqualTo(INVALID_DATA)

        job.cancel()
    }

    @Test
    fun `given data invalid, when validate failure, then expired is shown`() = runBlockingTest {
        initValidate(false, DATA_INVALID)

        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        viewModel.start()
        viewModel.onScanSuccess(SCANNED_VALUE)

        assertThat(result.last().type).isEqualTo(EXPIRED)

        job.cancel()
    }

    @Test
    fun `given validated state, when authenticate invoked, then authenticating followed by done`() = runBlockingTest {
        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        initAuthenticate()
        initAndStartVMForState(VALIDATED)

        val initialState = result.last()
        result.clear()
        (initialState as Validated).primaryActionButton.clickAction()

        assertThat(result.first().type).isEqualTo(AUTHENTICATING)
        assertThat(result.last().type).isEqualTo(DONE)

        job.cancel()
    }

    @Test
    fun `given not authorized error, when authenticate failure, then auth failed is shown`() = runBlockingTest {
        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        initAuthenticate(false, NOT_AUTHORIZED)
        initAndStartVMForState(VALIDATED)

        (result.last() as Validated).primaryActionButton.clickAction()

        assertThat(result.last().type).isEqualTo(AUTH_FAILED)

        job.cancel()
    }

    @Test
    fun `given error, when authenticate failure, then invalid data is shown`() = runBlockingTest {
        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        initAuthenticate(false, GENERIC_ERROR)
        initAndStartVMForState(VALIDATED)

        (result.last() as Validated).primaryActionButton.clickAction()

        assertThat(result.last().type).isEqualTo(INVALID_DATA)

        job.cancel()
    }

    @Test
    fun `given data invalid, when authenticate failure, then expired is shown`() = runBlockingTest {
        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        initAuthenticate(false, DATA_INVALID)
        initAndStartVMForState(VALIDATED)

        (result.last() as Validated).primaryActionButton.clickAction()

        assertThat(result.last().type).isEqualTo(EXPIRED)

        job.cancel()
    }

    @Test
    fun `given done, when primary action is clicked, then finish activity is raised`() = runBlockingTest {
        val result = mutableListOf<QRCodeAuthUiState>()
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        val job1 = launch {
            viewModel.uiState.toList(result)
        }
        val job2 = launch {
            viewModel.actionEvents.collect {
                actionEvents.add(it)
            }
        }

        initAndStartVMForState(DONE)

        (result.last() as Done).primaryActionButton.clickAction()

        assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.FinishActivity::class.java)

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun `given error, when primary action clicked, then launch scanner is raised`() = runBlockingTest {
        val result = mutableListOf<QRCodeAuthUiState>()
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        val job1 = launch {
            viewModel.uiState.toList(result)
        }
        val job2 = launch {
            viewModel.actionEvents.collect {
                actionEvents.add(it)
            }
        }

        initAndStartVMForState(INVALID_DATA)

        (result.last() as InvalidData).primaryActionButton.clickAction()

        assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.LaunchScanner::class.java)

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun `given error, when secondary action clicked, then finish activity is raised`() = runBlockingTest {
        val result = mutableListOf<QRCodeAuthUiState>()
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        val job1 = launch {
            viewModel.uiState.toList(result)
        }
        val job2 = launch {
            viewModel.actionEvents.collect {
                actionEvents.add(it)
            }
        }

        initAndStartVMForState(INVALID_DATA)

        (result.last() as InvalidData).secondaryActionButton.clickAction()

        assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.FinishActivity::class.java)

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun `given any state, when back is pressed, then dismiss dialog event is raised`() = runBlockingTest {
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        val job = launch {
            viewModel.actionEvents.collect {
                actionEvents.add(it)
            }
        }

        viewModel.onBackPressed()

        assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.LaunchDismissDialog::class.java)

        job.cancel()
    }

    @Test
    fun `when scan fails, then finish activity event is raised`() = runBlockingTest {
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        val job = launch {
            viewModel.actionEvents.collect {
                actionEvents.add(it)
            }
        }

        viewModel.onScanFailure()

        assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.FinishActivity::class.java)

        job.cancel()
    }

    @Test
    fun `given valid scan, when no network connection, then no internet error is shown`() = runBlockingTest {
        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        startViewModel()
        viewModel.onScanSuccess(SCANNED_VALUE)

        assertThat(result.last()).isInstanceOf(NoInternet::class.java)

        job.cancel()
    }

    @Test
    fun `given authenticating, when no network connection, then error view is shown`() = runBlockingTest {
        val result = mutableListOf<QRCodeAuthUiState>()
        val job = launch {
            viewModel.uiState.toList(result)
        }

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        initAndStartVMForState(VALIDATED)

        (result.last() as Validated).primaryActionButton.clickAction()

        assertThat(result.last().type).isEqualTo(NO_INTERNET)

        job.cancel()
    }

    @Test
    fun `given dismiss dialog showing, when ok clicked, then finish activity event is raised`() = runBlockingTest {
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        val job = launch {
            viewModel.actionEvents.collect {
                actionEvents.add(it)
            }
        }

        viewModel.onDialogInteraction(DialogInteraction.Positive("positive"))

        assertThat(actionEvents.last()).isInstanceOf(QRCodeAuthActionEvent.FinishActivity::class.java)

        job.cancel()
    }

    @Test
    fun `given dismiss dialog showing, when cancel clicked, then no event is raised`() = runBlockingTest {
        val actionEvents = mutableListOf<QRCodeAuthActionEvent>()
        val job = launch {
            viewModel.actionEvents.collect {
                actionEvents.add(it)
            }
        }

        viewModel.onDialogInteraction(DialogInteraction.Negative("negative"))

        assertThat(actionEvents.isEmpty())

        job.cancel()
    }

    private fun buildValidateError(errorType: QRCodeAuthErrorType) =
            QRCodeAuthResult<QRCodeAuthValidateResult>(QRCodeAuthError(errorType))

    private fun buildValidateSuccess() =
            QRCodeAuthResult(model = QRCodeAuthValidateResult(browser = BROWSER, location = LOCATION))

    private fun buildAuthenticateError(errorType: QRCodeAuthErrorType) =
            QRCodeAuthResult<QRCodeAuthAuthenticateResult>(QRCodeAuthError(errorType))

    private val authenticateSuccess = QRCodeAuthResult(model = QRCodeAuthAuthenticateResult(authenticated = true))

    private fun startViewModel(saveInstanceState: Bundle? = null) {
        viewModel.start(saveInstanceState)
    }

    private fun initAndStartVMForState(stateType: QRCodeAuthUiStateType) {
        initSavedInstanceState(stateType)
        viewModel.start(savedInstanceState)
    }

    private fun initSavedInstanceState(stateType: QRCodeAuthUiStateType) {
        whenever(savedInstanceState.getString(DATA_KEY, null)).thenReturn(DATA)
        whenever(savedInstanceState.getString(TOKEN_KEY, null)).thenReturn(TOKEN)
        whenever(savedInstanceState.getString(LOCATION_KEY, null)).thenReturn(LOCATION)
        whenever(savedInstanceState.getString(BROWSER_KEY, null)).thenReturn(BROWSER)
        whenever(savedInstanceState.getString(LAST_STATE_KEY, null)).thenReturn(stateType.label)
    }

    private suspend fun initValidate(
        successResponse: Boolean = true,
        errorType: QRCodeAuthErrorType? = INVALID_RESPONSE
    ) {
        whenever(store.validate(any(), any())).thenReturn(
                if (successResponse) {
                    buildValidateSuccess()
                } else {
                    buildValidateError(errorType as QRCodeAuthErrorType)
                }
        )
    }

    private suspend fun initAuthenticate(
        successResponse: Boolean = true,
        errorType: QRCodeAuthErrorType? = INVALID_RESPONSE
    ) {
        whenever(store.authenticate(any(), any())).thenReturn(
                if (successResponse) {
                    authenticateSuccess
                } else {
                    buildAuthenticateError(errorType as QRCodeAuthErrorType)
                }
        )
    }
}
