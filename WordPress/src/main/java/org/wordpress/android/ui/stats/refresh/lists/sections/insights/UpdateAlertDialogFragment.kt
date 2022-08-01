package org.wordpress.android.ui.stats.refresh.lists.sections.insights

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wordpress.android.R

class UpdateAlertDialogFragment : DialogFragment() {
    @Suppress("UseCheckOrError")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)

            builder.apply {
                setTitle(R.string.stats_revamp_v2_intro_header_title)
                setMessage(R.string.stats_revamp_v2_update_alert_message)
                setPositiveButton(R.string.ok) { _, _ ->
                    dismiss()
                }
                builder.setCancelable(true)
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        const val UPDATE_ALERT_DIALOG_TAG = "update_alert_dialog_tag"

        fun newInstance(): UpdateAlertDialogFragment {
            return UpdateAlertDialogFragment()
        }
    }
}
