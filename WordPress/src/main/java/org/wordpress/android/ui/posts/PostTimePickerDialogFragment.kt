package org.wordpress.android.ui.posts

import android.app.Dialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.wordpress.android.WordPress
import javax.inject.Inject

class PostTimePickerDialogFragment : DialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: EditPostPublishSettingsViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
                .get(EditPostPublishSettingsViewModel::class.java)

        val is24HrFormat = DateFormat.is24HourFormat(activity)
        val timePickerDialog = TimePickerDialog(activity,
                OnTimeSetListener { _, selectedHour, selectedMinute ->
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

        fun newInstance(): PostTimePickerDialogFragment {
            return PostTimePickerDialogFragment()
        }
    }
}
