package org.wordpress.android.ui.bloggingreminders

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.WordPress
import javax.inject.Inject

class BloggingReminderTimePicker : DialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: BloggingRemindersViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(BloggingRemindersViewModel::class.java)
        val time = viewModel.getSelectedTime()
        val is24HrFormat = DateFormat.is24HourFormat(activity)

        return TimePickerDialog(
            activity,
            { _, selectedHour, selectedMinute ->
                viewModel.onChangeTime(selectedHour, selectedMinute)
            },
            time.first,
            time.second,
            is24HrFormat
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    companion object {
        const val TAG = "blogging_reminders_time_picker"

        @JvmStatic
        fun newInstance() = BloggingReminderTimePicker()
    }
}
