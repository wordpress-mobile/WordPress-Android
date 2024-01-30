package org.wordpress.android.login.webauthn

import android.content.Context
import android.os.CancellationSignal
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import org.wordpress.android.fluxc.store.AccountStore.FinishWebauthnChallengePayload
import java.util.concurrent.Executors

class PasskeyRequest(
    context: Context,
    userId: String,
    twoStepNonce: String,
    requestJson: String,
    onSuccess: (FinishWebauthnChallengePayload) -> Unit,
    onFailure: (Throwable) -> Unit
) {
    init {
        val credentialManager = CredentialManager.create(context)
        val executor = Executors.newSingleThreadExecutor()
        val signal = CancellationSignal()
        val password = GetPasswordOption()
        val publicKeyCred = GetPublicKeyCredentialOption(requestJson)
        val getCredRequest = GetCredentialRequest(
                listOf(password, publicKeyCred)
        )

        try {
            credentialManager.getCredentialAsync(
                    request = getCredRequest,
                    context = context,
                    cancellationSignal = signal,
                    executor = executor,
                    callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                        override fun onError(e: GetCredentialException) {
                            onFailure(e)
                            Log.e("Credential Manager error", e.stackTraceToString())
                        }

                        override fun onResult(result: GetCredentialResponse) {
                            FinishWebauthnChallengePayload().apply {
                                mUserId = userId
                                mTwoStepNonce = twoStepNonce
                                mClientData = result.toJson().orEmpty()
                            }.let { onSuccess(it) }
                        }
                    }
            )
        } catch (e: GetCredentialException) {
            Log.e("Credential Manager error", e.stackTraceToString())
            onFailure(e)
        }
    }

    private fun GetCredentialResponse.toJson(): String? {
        return when (val credential = this.credential) {
            is PublicKeyCredential -> credential.authenticationResponseJson
            else -> {
                Log.e("Credential Manager", "Unexpected type of credential")
                null
            }
        }
    }
}
