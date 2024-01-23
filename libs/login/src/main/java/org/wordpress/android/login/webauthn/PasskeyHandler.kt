package org.wordpress.android.login.webauthn

import android.app.Activity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.fragment.app.Fragment

class PasskeyHandler {
    fun fetch(requestingFragment: Fragment, onResult: (PasskeyDataResult) -> Unit) {
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

    data class PasskeyDataResult(
        val isFailure: Boolean
    )
}
