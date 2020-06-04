package org.wordpress.android.ui.posts

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.wordpress.android.R
import org.wordpress.android.WordPress
import javax.inject.Inject

class PostDatePickerDialogFragment : DialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: EditPostPublishSettingsViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
                .get(EditPostPublishSettingsViewModel::class.java)

        val datePickerDialog = DatePickerDialog(activity,
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

        fun newInstance(): PostDatePickerDialogFragment {
            return PostDatePickerDialogFragment()
        }
    }
}
