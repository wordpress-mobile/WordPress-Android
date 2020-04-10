package org.wordpress.android.ui

import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import org.wordpress.android.R

class PrivateAtCookieRefreshProgressDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogMessage = activity?.getString(R.string.media_accessing_progress)
        return ProgressDialog.show(
                activity, "", dialogMessage, true, true
        )
    }

    fun isDialogVisible(): Boolean {
        return dialog != null && dialog!!.isShowing
    }

    companion object {
        const val TAG = "private_at_cookie_progress_dialog"

        fun showIfNecessary(fragmentManager: FragmentManager?) {
            fragmentManager.let {
                val thisFragment = fragmentManager!!.findFragmentByTag(TAG)
                if (thisFragment == null ||
                        (thisFragment is PrivateAtCookieRefreshProgressDialog && !thisFragment.isDialogVisible())) {
                    PrivateAtCookieRefreshProgressDialog().show(fragmentManager, TAG)
                }
            }
        }

        fun dismissIfNecessary(fragmentManager: FragmentManager?) {
            fragmentManager.let {
                val thisFragment = fragmentManager!!.findFragmentByTag(TAG)
                if (thisFragment is PrivateAtCookieRefreshProgressDialog) {
                    thisFragment.dismiss()
                }
            }
        }
    }
}
