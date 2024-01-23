package org.wordpress.android.login.webauthn

import android.content.Context
import android.util.Base64
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType.PUBLIC_KEY
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnChallengeInfo
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnCredentialResponse
import org.wordpress.android.fluxc.store.AccountStore.FinishWebauthnChallengePayload

interface OnPasskeyRequestReadyListener {
    fun onPasskeyRequestReady(intentSenderRequest: IntentSenderRequest)
}

class Fido2ClientHandler(
    private val userId: String,
    private val challengeInfo: WebauthnChallengeInfo
) {
    fun createIntentSender(
        context: Context,
        listener: OnPasskeyRequestReadyListener
    ) {
        val options = PublicKeyCredentialRequestOptions.Builder()
                .setRpId(challengeInfo.rpId)
                .setAllowList(challengeInfo.allowCredentials.map(::parseToCredentialDescriptor))
                .setChallenge(challengeInfo.challenge.decodeBase64())
                .setTimeoutSeconds(challengeInfo.timeout.toDouble())
                .build()

        Fido.getFido2ApiClient(context)
                .getSignPendingIntent(options)
                .addOnSuccessListener {
                    val intentSender = IntentSenderRequest
                            .Builder(it.intentSender)
                            .build()

                    listener.onPasskeyRequestReady(intentSender)
                }
    }

    fun onCredentialsAvailable(keyCredential: PublicKeyCredential): FinishWebauthnChallengePayload {
        return FinishWebauthnChallengePayload().apply {
            this.mUserId = userId
            this.mTwoStepNonce = challengeInfo.twoStepNonce
            this.mClientData = keyCredential.toJson()
        }
    }

    private fun parseToCredentialDescriptor(credential: WebauthnCredentialResponse) =
            PublicKeyCredentialDescriptor(
                    PUBLIC_KEY.toString(),
                    credential.id.decodeBase64(),
                    credential.transports.asParsedTransports()
            )

    private fun String.decodeBase64(): ByteArray {
        return Base64.decode(this, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE)
    }

    private fun List<String>.asParsedTransports(): List<Transport> {
        return mapNotNull {
            when (it) {
                "usb" -> Transport.USB
                "nfc" -> Transport.NFC
                "ble" -> Transport.BLUETOOTH_LOW_ENERGY
                "internal" -> Transport.INTERNAL
                "hybrid" -> Transport.HYBRID
                else -> null
            }
        }.ifEmpty {
            listOf(Transport.USB, Transport.NFC, Transport.BLUETOOTH_LOW_ENERGY, Transport.HYBRID, Transport.INTERNAL)
        }
    }
}
