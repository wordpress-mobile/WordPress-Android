package org.wordpress.android.ui


import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class PrivateAtCookieRefreshProgressDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
       return ProgressDialog.show(
                activity, "", "Establishing access to media files on private site.", true, true
        )
    }

    companion object {
        const val TAG = "private_at_cookie_progress_dialog"
    }
}
