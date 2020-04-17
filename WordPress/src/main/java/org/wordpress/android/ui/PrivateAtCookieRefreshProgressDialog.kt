package org.wordpress.android.ui

import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.wordpress.android.R

class PrivateAtCookieRefreshProgressDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogMessage = activity?.getString(R.string.media_accessing_progress)
        return ProgressDialog.show(
                activity, "", dialogMessage, true, true
        )
    }

    interface PrivateAtCookieProgressDialogOnDismissListener {
        fun onCookieProgressDialogCancelled()
    }

    fun isDialogVisible(): Boolean {
        return dialog != null && dialog!!.isShowing
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (targetFragment is PrivateAtCookieProgressDialogOnDismissListener) {
            (targetFragment as PrivateAtCookieProgressDialogOnDismissListener).onCookieProgressDialogCancelled()
        } else if (activity is PrivateAtCookieProgressDialogOnDismissListener) {
            (activity as PrivateAtCookieProgressDialogOnDismissListener).onCookieProgressDialogCancelled()
        }
    }

    companion object {
        const val TAG = "private_at_cookie_progress_dialog"

        fun showIfNecessary(fragmentManager: FragmentManager?) {
            showIfNecessary(fragmentManager, null)
        }

        fun showIfNecessary(fragmentManager: FragmentManager?, targetFragment: Fragment?) {
            fragmentManager?.let {
                val thisFragment = fragmentManager.findFragmentByTag(TAG)
                if (thisFragment == null ||
                        (thisFragment is PrivateAtCookieRefreshProgressDialog && !thisFragment.isDialogVisible())) {
                    val progressFragment = PrivateAtCookieRefreshProgressDialog()
                    progressFragment.setTargetFragment(targetFragment, 0)
                    progressFragment.show(fragmentManager, TAG)
                }
            }
        }

        fun dismissIfNecessary(fragmentManager: FragmentManager?) {
            fragmentManager?.let {
                val thisFragment = fragmentManager.findFragmentByTag(TAG)
                if (thisFragment is PrivateAtCookieRefreshProgressDialog) {
                    thisFragment.dismiss()
                }
            }
        }

        fun isShowing(fragmentManager: FragmentManager?): Boolean {
            fragmentManager?.let {
                val thisFragment = fragmentManager.findFragmentByTag(TAG)
                return thisFragment is PrivateAtCookieRefreshProgressDialog && thisFragment.isDialogVisible()
            }
            return false
        }
    }
}
