package org.wordpress.android.ui.comments.unified

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wordpress.android.R
import org.wordpress.android.WordPress
import javax.inject.Inject

class EditCancelDialogFragment : DialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var viewModel: UnifiedCommentsEditViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        (parentFragment as? UnifiedCommentsEditFragment)?.let {
            viewModel = ViewModelProvider(
                it,
                viewModelFactory
            ).get(UnifiedCommentsEditViewModel::class.java)
        }

        builder.apply {
            setTitle(R.string.comment_edit_cancel_dialog_title)
            setMessage(R.string.comment_edit_cancel_dialog_message)
            setPositiveButton(R.string.button_discard) { _, _ ->
                viewModel?.onConfirmEditingDiscard()
            }
            setNegativeButton(R.string.cancel) { _, _ ->
                // nothing to be done here.
            }
            builder.setCancelable(true)
        }
        return builder.create()
    }

    companion object {
        const val EDIT_CANCEL_DIALOG_TAG = "edit_cancel_dialog_tag"

        fun newInstance(): EditCancelDialogFragment {
            return EditCancelDialogFragment()
        }
    }
}
