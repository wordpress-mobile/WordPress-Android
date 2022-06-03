package org.wordpress.android.ui.qrcodeauth

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.AuthenticatingPrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.AuthenticatingSecondaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.DonePrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.DoneSecondaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.ErrorPrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.ErrorSecondaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.ValidatedPrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.ValidatedSecondaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Content.Authenticating
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Content.Done
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Content.Validated
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Error.AuthFailed
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Error.Expired
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Error.InvalidData
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Error.NoInternet
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Loading
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Scanning

class QRCodeAuthUiStateMapperTest {
    private val mapper = QRCodeAuthUiStateMapper()

    @Test
    fun `when loading requested, then loading should be returned`() {
        val actual = mapper.mapLoading()
        val expected = Loading
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when scanning requested, then scanning should be returned`() {
        val actual = mapper.mapScanning()
        val expected = Scanning
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when auth failed requested, then auth failed should be returned`() {
        val actual = mapper.mapAuthFailed(primaryClickAction, secondaryClickAction)
        val expected = authFailedExpected
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when expired requested, then expired should be returned`() {
        val actual = mapper.mapExpired(primaryClickAction, secondaryClickAction)
        val expected = expiredExpected
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when invalid data requested, then invalid data should be returned`() {
        val actual = mapper.mapInvalidData(primaryClickAction, secondaryClickAction)
        val expected = invalidDataExpected
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when no internet requested, then no internet should be returned`() {
        val actual = mapper.mapNoInternet(primaryClickAction, secondaryClickAction)
        val expected = noInternetExpected
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when validated requested, then validated should be returned`() {
        val actual = mapper.mapValidated(location, browser, primaryClickAction, secondaryClickAction)
        val expected = validatedExpected
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when no authenticating requested, then authenticating should be returned`() {
        val actual = mapper.mapAuthenticating(location = location, browser = browser)
        val expected = authenticatingExpected
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when done requested, then done should be returned`() {
        val actual = mapper.mapDone(primaryClickAction)
        val expected = doneExpected
        assertThat(actual).isEqualTo(expected)
    }

    private val primaryClickAction: () -> Unit = {}
    private val secondaryClickAction: () -> Unit = {}
    private val browser = "browser"
    private val location = "location"
    private val authFailedExpected = AuthFailed(
            primaryAction = ErrorPrimaryAction(primaryClickAction),
            secondaryAction = ErrorSecondaryAction(secondaryClickAction))

    private val expiredExpected = Expired(
            primaryAction = ErrorPrimaryAction(primaryClickAction),
            secondaryAction = ErrorSecondaryAction(secondaryClickAction))

    private val invalidDataExpected = InvalidData(
            primaryAction = ErrorPrimaryAction(primaryClickAction),
            secondaryAction = ErrorSecondaryAction(secondaryClickAction))

    private val noInternetExpected = NoInternet(
            primaryAction = ErrorPrimaryAction(primaryClickAction),
            secondaryAction = ErrorSecondaryAction(secondaryClickAction))

    private val validatedExpected = Validated(
            primaryAction = ValidatedPrimaryAction(primaryClickAction),
            secondaryAction = ValidatedSecondaryAction(secondaryClickAction),
            location = location,
            browser = browser)

    private val authenticatingExpected = Authenticating(
            primaryAction = AuthenticatingPrimaryAction,
            secondaryAction = AuthenticatingSecondaryAction,
            location = location,
            browser = browser)

    private val doneExpected = Done(
            primaryAction = DonePrimaryAction(primaryClickAction),
            secondaryAction = DoneSecondaryAction
    )
}
