package org.wordpress.android.login.webauthn

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.fragment.app.Fragment
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnChallengeInfo

class PasskeyHandler {
    fun fetch(
        requestingFragment: Fragment,
        userId: String,
        challengeInfo: WebauthnChallengeInfo,
        onResult: (PasskeyDataResult) -> Unit
    ) {
        requestingFragment.registerForActivityResult(
                StartIntentSenderForResult()
        )
        { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                onResult(PasskeyDataResult(isFailure = false))
            } else {
                onResult(PasskeyDataResult(isFailure = true))
            }
        }
    }

    private fun parseFIDO2IntentData(
        resultData: Intent,
        userId: String,
        challengeInfo: WebauthnChallengeInfo
    ): PasskeyDataResult? =
            resultData.takeIf { it.hasExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA) }
                    ?.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
                    ?.let { PublicKeyCredential.deserializeFromBytes(it).toJson() }
                    ?.let { clientData ->
                        PasskeyDataResult(
                                isFailure = false,
                                userId = userId,
                                twoStepNonce = challengeInfo.twoStepNonce,
                                clientData = clientData
                        )
                    }

    data class PasskeyDataResult(
        val isFailure: Boolean,
        val userId: String,
        val twoStepNonce: String,
        val clientData: String
    )
}
