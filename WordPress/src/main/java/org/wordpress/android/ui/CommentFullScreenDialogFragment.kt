package org.wordpress.android.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import org.wordpress.android.R
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.CollapseFullScreenDialogContent
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.CollapseFullScreenDialogController
import org.wordpress.android.widgets.SuggestionAutoCompleteText

class CommentFullScreenDialogFragment : Fragment(), CollapseFullScreenDialogContent {

    private lateinit var mDialogController: CollapseFullScreenDialogController
    private lateinit var mReply: SuggestionAutoCompleteText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.comment_dialog_fragment, container, false) as ViewGroup
        mReply = layout.findViewById(R.id.edit_comment_expand)
        mReply.requestFocus()
        mReply.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                mDialogController.setConfirmEnabled(s.isNotEmpty())
            }
        })

        arguments?.let {
            mReply.setText(it.getString(EXTRA_REPLY))
            mReply.setSelection(it.getInt(EXTRA_SELECTION_START), it.getInt(EXTRA_SELECTION_END))
        }

        return layout
    }

    override fun onCollapseClicked(controller: CollapseFullScreenDialogController): Boolean {
        controller.collapse(saveResult())
        return true
    }

    override fun onConfirmClicked(controller: CollapseFullScreenDialogController): Boolean {
        controller.confirm(saveResult())
        return true
    }

    private fun saveResult(): Bundle {
        val result = Bundle()
        result.putString(RESULT_REPLY, mReply.text.toString())
        result.putInt(RESULT_SELECTION_START, mReply.selectionStart)
        result.putInt(RESULT_SELECTION_END, mReply.selectionEnd)
        return result
    }

    override fun onViewCreated(controller: CollapseFullScreenDialogController) {
        mDialogController = controller
    }

    companion object {
        const val RESULT_REPLY = "RESULT_REPLY"
        const val RESULT_SELECTION_START = "RESULT_SELECTION_START"
        const val RESULT_SELECTION_END = "RESULT_SELECTION_END"

        private const val EXTRA_REPLY = "EXTRA_REPLY"
        private const val EXTRA_SELECTION_START = "EXTRA_SELECTION_START"
        private const val EXTRA_SELECTION_END = "EXTRA_SELECTION_END"

        fun newBundle(reply: String, selectionStart: Int, selectionEnd: Int): Bundle {
            val bundle = Bundle()
            bundle.putString(EXTRA_REPLY, reply)
            bundle.putInt(EXTRA_SELECTION_START, selectionStart)
            bundle.putInt(EXTRA_SELECTION_END, selectionEnd)
            return bundle
        }
    }
}
