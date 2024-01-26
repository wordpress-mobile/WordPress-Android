package org.wordpress.android.login.webauthn

import android.content.Context
import android.os.CancellationSignal
import android.util.Log
import androidx.annotation.RequiresApi
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

class CredentialManagerHandler(
    private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)
    private val executor = Executors.newSingleThreadExecutor()

    fun fetchPasskey(
        userId: String,
        twoStepNonce: String,
        requestJson: String,
        onSuccess: (FinishWebauthnChallengePayload) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val password = GetPasswordOption()
        val publicKeyCred = GetPublicKeyCredentialOption(requestJson)
        val getCredRequest = GetCredentialRequest(
                listOf(password, publicKeyCred)
        )

        val signal = CancellationSignal()

        try {
            credentialManager.getCredentialAsync(
                    request = getCredRequest,
                    context = context,
                    cancellationSignal = signal,
                    executor = executor,
                    callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                        override fun onError(e: GetCredentialException) {
                            onFailure(e)
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
            Log.e("Error", e.stackTraceToString())
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
