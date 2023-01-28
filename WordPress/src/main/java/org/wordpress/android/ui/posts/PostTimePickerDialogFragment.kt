package org.wordpress.android.ui.posts

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R.style
import org.wordpress.android.WordPress

import org.wordpress.android.ui.posts.prepublishing.PrepublishingPublishSettingsViewModel
import org.wordpress.android.util.extensions.getParcelableCompat
import javax.inject.Inject

class PostTimePickerDialogFragment : DialogFragment() {
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

        val is24HrFormat = DateFormat.is24HourFormat(activity)
        val context = ContextThemeWrapper(activity, style.PostSettingsCalendar)
        val timePickerDialog = TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                viewModel.onTimeSelected(selectedHour, selectedMinute)
            },
            viewModel.hour ?: 0,
            viewModel.minute ?: 0,
            is24HrFormat
        )
        return timePickerDialog
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    companion object {
        const val TAG = "post_time_picker_dialog_fragment"
        const val ARG_PUBLISH_SETTINGS_FRAGMENT_TYPE = "publish_settings_fragment_type"

        fun newInstance(publishSettingsFragmentType: PublishSettingsFragmentType): PostTimePickerDialogFragment {
            return PostTimePickerDialogFragment().apply {
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
