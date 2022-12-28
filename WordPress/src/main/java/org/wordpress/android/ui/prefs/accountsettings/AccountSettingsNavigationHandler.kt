package org.wordpress.android.ui.prefs.accountsettings

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.R
import org.wordpress.android.ui.FullScreenDialogFragment
import org.wordpress.android.ui.accounts.signup.BaseUsernameChangerFullScreenDialogFragment
import org.wordpress.android.ui.accounts.signup.SettingsUsernameChangerFragment
import javax.inject.Inject

class AccountSettingsNavigationHandler @Inject constructor() {
    fun showUsernameChangerScreen(
        activity: Activity,
        request: UserNameChangeScreenRequest
    ) {
        with(request) {
            val bundle: Bundle = BaseUsernameChangerFullScreenDialogFragment.newBundle(displayName, userName)
            FullScreenDialogFragment.Builder(activity)
                .setTitle(R.string.username_changer_title)
                .setAction(R.string.username_changer_action)
                .setOnConfirmListener { result -> onConfirm.invoke(result) }
                .setIsLifOnScroll(false)
                .setOnDismissListener { onDismiss.invoke() }
                .setOnShownListener { onShown.invoke() }
                .setContent(SettingsUsernameChangerFragment::class.java, bundle)
                .build()
                .show(
                    (activity as AppCompatActivity).supportFragmentManager,
                    FullScreenDialogFragment.TAG
                )
        }
    }
}

data class UserNameChangeScreenRequest(
    val userName: String,
    val displayName: String,
    val onConfirm: (Bundle?) -> Unit?,
    val onDismiss: () -> Unit,
    val onShown: () -> Unit
)
