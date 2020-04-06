package org.wordpress.android.ui.publicize

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wordpress.android.R
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.publicize.PublicizeConstants.PUBLICIZE_FACEBOOK_SHARING_SUPPORT_LINK
import java.lang.NullPointerException

class PublicizeErrorDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            it.getInt(REASON_RES_ID).let { reasonResId ->
                // the custom dialog behavior here is specific to a user who is trying to share to Facebook
                // but does not have any available Facebook Pages.
                if (reasonResId == R.string.sharing_facebook_account_must_have_pages) {
                    return MaterialAlertDialogBuilder(activity).apply {
                        setTitle(R.string.dialog_title_sharing_facebook_account_must_have_pages)
                        setMessage(R.string.sharing_facebook_account_must_have_pages)
                        setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                        setNegativeButton(R.string.learn_more) { _, _ ->
                            WPWebViewActivity.openURL(
                                    activity,
                                    PUBLICIZE_FACEBOOK_SHARING_SUPPORT_LINK
                            )
                        }
                    }.create()
                } else {
                    return MaterialAlertDialogBuilder(activity).apply {
                        setMessage(reasonResId)
                        setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    }.create()
                }
            }
        } ?: throw NullPointerException("Missing argument. Utilize newInstance() to instantiate.")
    }

    companion object {
        const val REASON_RES_ID = "REASON_RES_ID"
        const val TAG = "publicize_error_fragment"

        @JvmStatic
        fun newInstance(@StringRes reasonId: Int) = PublicizeErrorDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(REASON_RES_ID, reasonId)
            }
        }
    }
}
