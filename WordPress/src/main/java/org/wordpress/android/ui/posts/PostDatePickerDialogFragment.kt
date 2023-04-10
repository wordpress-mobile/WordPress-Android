package org.wordpress.android.ui.posts

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.prepublishing.PrepublishingPublishSettingsViewModel
import org.wordpress.android.util.extensions.getParcelableCompat
import javax.inject.Inject

class PostDatePickerDialogFragment : DialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PublishSettingsViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val publishSettingsFragmentType = arguments?.getParcelableCompat<PublishSettingsFragmentType>(
            ARG_PUBLISH_SETTINGS_FRAGMENT_TYPE
        )

        viewModel = when (publishSettingsFragmentType) {
            PublishSettingsFragmentType.EDIT_POST -> ViewModelProvider(
                requireActivity(),
                viewModelFactory
            )[EditPostPublishSettingsViewModel::class.java]
            PublishSettingsFragmentType.PREPUBLISHING_NUDGES -> ViewModelProvider(
                requireActivity(),
                viewModelFactory
            )[PrepublishingPublishSettingsViewModel::class.java]
            null -> error("PublishSettingsViewModel not initialized")
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.PostSettingsCalendar,
            null,
            viewModel.year ?: 0,
            viewModel.month ?: 0,
            viewModel.day ?: 0
        )
        datePickerDialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            getString(android.R.string.ok)
        ) { _, _ ->
            viewModel.onDateSelected(
                datePickerDialog.datePicker.year,
                datePickerDialog.datePicker.month,
                datePickerDialog.datePicker.dayOfMonth
            )
        }
        val neutralButtonTitle = if (viewModel.canPublishImmediately)
            getString(R.string.immediately)
        else
            getString(R.string.now)
        datePickerDialog.setButton(
            DialogInterface.BUTTON_NEUTRAL,
            neutralButtonTitle
        ) { _, _ ->
            viewModel.publishNow()
        }
        return datePickerDialog
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    companion object {
        const val TAG = "post_date_picker_dialog_fragment"
        private const val ARG_PUBLISH_SETTINGS_FRAGMENT_TYPE = "publish_settings_fragment_type"

        fun newInstance(publishSettingsFragmentType: PublishSettingsFragmentType): PostDatePickerDialogFragment {
            return PostDatePickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(
                        ARG_PUBLISH_SETTINGS_FRAGMENT_TYPE,
                        publishSettingsFragmentType
                    )
                }
            }
        }
    }
}
