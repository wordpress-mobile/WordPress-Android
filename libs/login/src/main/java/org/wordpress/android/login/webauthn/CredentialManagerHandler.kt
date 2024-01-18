package org.wordpress.android.login.webauthn

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.GetCredentialException

class CredentialManagerHandler(
    private val context: Context
) {
    val credentialManager = CredentialManager.create(context)

    @RequiresApi(34)
    private fun CredentialManager.createPasskey(
        context: Context,
        requestJson: String
    ): GetCredentialResponse? {
        val password = GetPasswordOption()
        val publicKeyCred = GetPublicKeyCredentialOption(requestJson)
        val getCredRequest = GetCredentialRequest(
                listOf(password, publicKeyCred)
        )

        return try {
            getCredentialAsync(
                    request = getCredRequest,
                    context = context,
                    callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        override fun onError(e: GetCredentialException) {
                            TODO("Not yet implemented")
                        }

                        override fun onResult(result: GetCredentialResponse) {
                            TODO("Not yet implemented")
                        }
                    }
            )
        } catch (e: GetCredentialException) {
            Log.e("Error", e.stackTraceToString())
            null
        }
    }
}
