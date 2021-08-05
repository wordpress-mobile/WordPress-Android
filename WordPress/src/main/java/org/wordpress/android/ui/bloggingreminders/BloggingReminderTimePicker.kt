package org.wordpress.android.ui.bloggingreminders

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.WordPress
import java.util.Calendar
import javax.inject.Inject

class BloggingReminderTimePicker : DialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: BloggingRemindersViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(BloggingRemindersViewModel::class.java)
        // Use the current time as the default values for the picker
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val is24HrFormat = DateFormat.is24HourFormat(activity)
        return TimePickerDialog(
                activity,
                { _, selectedHour, selectedMinute ->
                    viewModel.onChangeTime(selectedHour, selectedMinute)
                },
                hour,
                minute,
                is24HrFormat)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    companion object {
        const val TAG = "blogging_reminders_time_picker"
        private const val ARG_HOUR = "arg_hour"
        private const val ARG_MINUTE = "arg_minute"

        fun newInstance(hour: Int, minute: Int): BloggingReminderTimePicker {
            return BloggingReminderTimePicker().apply {
                arguments = Bundle().apply {
                    putInt(ARG_HOUR, hour)
                    putInt(ARG_MINUTE, minute)
                }
            }
        }
    }
}
