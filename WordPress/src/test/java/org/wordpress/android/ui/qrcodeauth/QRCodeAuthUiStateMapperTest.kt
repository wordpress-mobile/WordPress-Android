package org.wordpress.android.ui.qrcodeauth

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.AuthenticatingPrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.AuthenticatingSecondaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.DonePrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.DoneSecondaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ErrorPrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ErrorSecondaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ValidatedPrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ValidatedSecondaryActionButton
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
        val actual = mapper.mapToLoading()
        val expected = Loading
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when scanning requested, then scanning should be returned`() {
        val actual = mapper.mapToScanning()
        val expected = Scanning
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when auth failed requested, then auth failed should be returned`() {
        val actual = mapper.mapToAuthFailed(primaryClickAction, secondaryClickAction)
        val expected = authFailedExpected
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when expired requested, then expired should be returned`() {
        val actual = mapper.mapToExpired(primaryClickAction, secondaryClickAction)
        val expected = expiredExpected
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when invalid data requested, then invalid data should be returned`() {
        val actual = mapper.mapToInvalidData(primaryClickAction, secondaryClickAction)
        val expected = invalidDataExpected
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when no internet requested, then no internet should be returned`() {
        val actual = mapper.mapToNoInternet(primaryClickAction, secondaryClickAction)
        val expected = noInternetExpected
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when validated requested, then validated should be returned`() {
        val actual = mapper.mapToValidated(location, browser, primaryClickAction, secondaryClickAction)
        val expected = validatedExpected
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when no authenticating requested, then authenticating should be returned`() {
        val actual = mapper.mapToAuthenticating(location = location, browser = browser)
        val expected = authenticatingExpected
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when done requested, then done should be returned`() {
        val actual = mapper.mapToDone(primaryClickAction)
        val expected = doneExpected
        assertThat(actual).isEqualTo(expected)
    }

    private val primaryClickAction: () -> Unit = {}
    private val secondaryClickAction: () -> Unit = {}
    private val browser = "browser"
    private val location = "location"
    private val authFailedExpected = AuthFailed(
            primaryActionButton = ErrorPrimaryActionButton(primaryClickAction),
            secondaryActionButton = ErrorSecondaryActionButton(secondaryClickAction))

    private val expiredExpected = Expired(
            primaryActionButton = ErrorPrimaryActionButton(primaryClickAction),
            secondaryActionButton = ErrorSecondaryActionButton(secondaryClickAction))

    private val invalidDataExpected = InvalidData(
            primaryActionButton = ErrorPrimaryActionButton(primaryClickAction),
            secondaryActionButton = ErrorSecondaryActionButton(secondaryClickAction))

    private val noInternetExpected = NoInternet(
            primaryActionButton = ErrorPrimaryActionButton(primaryClickAction),
            secondaryActionButton = ErrorSecondaryActionButton(secondaryClickAction))

    private val validatedExpected = Validated(
            primaryActionButton = ValidatedPrimaryActionButton(primaryClickAction),
            secondaryActionButton = ValidatedSecondaryActionButton(secondaryClickAction),
            location = location,
            browser = browser)

    private val authenticatingExpected = Authenticating(
            primaryActionButton = AuthenticatingPrimaryActionButton,
            secondaryActionButton = AuthenticatingSecondaryActionButton,
            location = location,
            browser = browser)

    private val doneExpected = Done(
            primaryActionButton = DonePrimaryActionButton(primaryClickAction),
            secondaryActionButton = DoneSecondaryActionButton
    )
}
