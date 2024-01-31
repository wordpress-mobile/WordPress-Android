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

class PasskeyRequest private constructor(
    context: Context,
    requestData: PasskeyRequestData,
    onSuccess: (FinishWebauthnChallengePayload) -> Unit,
    onFailure: (Throwable) -> Unit
) {
    init {
        val executor = Executors.newSingleThreadExecutor()
        val signal = CancellationSignal()
        val getCredRequest = GetCredentialRequest(
                listOf(GetPasswordOption(), GetPublicKeyCredentialOption(requestData.requestJson))
        )

        val passkeyRequestCallback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
            override fun onError(e: GetCredentialException) {
                onFailure(e)
                Log.e(TAG, e.stackTraceToString())
            }

            override fun onResult(result: GetCredentialResponse) {
                FinishWebauthnChallengePayload().apply {
                    mUserId = requestData.userId
                    mTwoStepNonce = requestData.twoStepNonce
                    mClientData = result.toJson().orEmpty()
                }.let { onSuccess(it) }
            }
        }

        try {
            CredentialManager.create(context).getCredentialAsync(
                    request = getCredRequest,
                    context = context,
                    cancellationSignal = signal,
                    executor = executor,
                    callback = passkeyRequestCallback
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, e.stackTraceToString())
            onFailure(e)
        }
    }

    private fun GetCredentialResponse.toJson(): String? {
        return when (val credential = this.credential) {
            is PublicKeyCredential -> credential.authenticationResponseJson
            else -> {
                Log.e(TAG, "Unexpected type of credential")
                null
            }
        }
    }

    data class PasskeyRequestData(
        val userId: String,
        val twoStepNonce: String,
        val requestJson: String
    )

    companion object {
        const val TAG = "PasskeyRequest"
        fun create(
            context: Context,
            requestData: PasskeyRequestData,
            onSuccess: (FinishWebauthnChallengePayload) -> Unit,
            onFailure: (Throwable) -> Unit
        ) {
            PasskeyRequest(context, requestData, onSuccess, onFailure)
        }
    }
}
