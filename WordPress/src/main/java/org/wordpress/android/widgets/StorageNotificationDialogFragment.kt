package org.wordpress.android.widgets

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.analytics.AnalyticsUtils

class StorageNotificationDialogFragment : DialogFragment() {
    data class DialogLabels(
        val title: String,
        val message: String,
        val okLabel: String,
        val dontShowAgainLabel: String
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()

        val title = args.getString(ARG_DIALOG_TITLE)
        val message = args.getString(ARG_DIALOG_MESSAGE)
        val okLabel = args.getString(ARG_DIALOG_OK_LABEL)
        val dontShowAgainLabel = args.getString(ARG_DIALOG_DONT_SHOW_LABEL)
        val isInternalStorageSettingsResolved = args.getBoolean(ARG_DIALOG_IS_SETTINGS_RESOLVED)
        val sourceDescription = args.getString(ARG_DIALOG_SOURCE_DESCRIPTION)

        val builder = MaterialAlertDialogBuilder(requireContext())

        builder.apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton(okLabel) { _, _ ->
                dismiss()
                if (isInternalStorageSettingsResolved) {
                    val settingsIntent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                    val dialogContext = this@StorageNotificationDialogFragment.context
                    dialogContext?.startActivity(settingsIntent)
                }
                AnalyticsUtils.trackStorageWarningDialogEvent(
                    Stat.STORAGE_WARNING_ACKNOWLEDGED,
                    sourceDescription,
                    isInternalStorageSettingsResolved
                )
            }
            setNeutralButton(dontShowAgainLabel) { _, _ ->
                dismiss()
                AppPrefs.setShouldShowStorageWarning(false)
                AnalyticsUtils.trackStorageWarningDialogEvent(
                    Stat.STORAGE_WARNING_DONT_SHOW_AGAIN,
                    sourceDescription,
                    isInternalStorageSettingsResolved
                )
            }
            if (isInternalStorageSettingsResolved) {
                setNegativeButton(android.R.string.cancel) { _, _ ->
                    dismiss()
                    AnalyticsUtils.trackStorageWarningDialogEvent(
                        Stat.STORAGE_WARNING_CANCELED,
                        sourceDescription,
                        isInternalStorageSettingsResolved
                    )
                }
            }
        }

        return builder.create()
    }

    override fun onCancel(dialog: DialogInterface) {
        val args = requireArguments()
        val isInternalStorageSettingsResolved = args.getBoolean(ARG_DIALOG_IS_SETTINGS_RESOLVED)
        val sourceDescription = args.getString(ARG_DIALOG_SOURCE_DESCRIPTION)

        AnalyticsUtils.trackStorageWarningDialogEvent(
            Stat.STORAGE_WARNING_CANCELED,
            sourceDescription,
            isInternalStorageSettingsResolved
        )
        super.onCancel(dialog)
    }

    companion object {
        private const val ARG_DIALOG_TITLE = "dialog_title"
        private const val ARG_DIALOG_MESSAGE = "dialog_message"
        private const val ARG_DIALOG_OK_LABEL = "dialog_ok_label"
        private const val ARG_DIALOG_DONT_SHOW_LABEL = "dialog_dont_show_label"
        private const val ARG_DIALOG_IS_SETTINGS_RESOLVED = "dialog_is_settings_resolved"
        private const val ARG_DIALOG_SOURCE_DESCRIPTION = "dialog_source_description"

        fun newInstance(
            dialogLabels: DialogLabels,
            isInternalStorageSettingsResolved: Boolean,
            source: String
        ): StorageNotificationDialogFragment {
            AnalyticsUtils.trackStorageWarningDialogEvent(
                Stat.STORAGE_WARNING_SHOWN,
                source,
                isInternalStorageSettingsResolved
            )

            return StorageNotificationDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DIALOG_TITLE, dialogLabels.title)
                    putString(ARG_DIALOG_MESSAGE, dialogLabels.message)
                    putString(ARG_DIALOG_OK_LABEL, dialogLabels.okLabel)
                    putString(ARG_DIALOG_DONT_SHOW_LABEL, dialogLabels.dontShowAgainLabel)
                    putBoolean(ARG_DIALOG_IS_SETTINGS_RESOLVED, isInternalStorageSettingsResolved)
                    putString(ARG_DIALOG_SOURCE_DESCRIPTION, source)
                }
            }
        }
    }
}
