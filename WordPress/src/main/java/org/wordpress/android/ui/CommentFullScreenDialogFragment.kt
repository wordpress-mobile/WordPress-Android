package org.wordpress.android.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.CollapseFullScreenDialogContent
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.CollapseFullScreenDialogController
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager
import org.wordpress.android.ui.suggestion.util.SuggestionUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.widgets.SuggestionAutoCompleteText
import javax.inject.Inject

class CommentFullScreenDialogFragment : Fragment(), CollapseFullScreenDialogContent {
    @Inject lateinit var viewModel: CommentFullScreenDialogViewModel
    private lateinit var dialogController: CollapseFullScreenDialogController
    private lateinit var reply: SuggestionAutoCompleteText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.comment_dialog_fragment, container, false) as ViewGroup
        reply = layout.findViewById(R.id.edit_comment_expand)
        reply.requestFocus()
        reply.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                dialogController.setConfirmEnabled(s.isNotEmpty())
            }
        })

        viewModel.onKeyboardOpened.observe(this, Observer {
            it?.applyIfNotHandled {
                GlobalScope.launch {
                    val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showSoftInput(reply, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        })

        arguments?.let {
            reply.setText(it.getString(EXTRA_REPLY))
            reply.setSelection(it.getInt(EXTRA_SELECTION_START), it.getInt(EXTRA_SELECTION_END))
            viewModel.init()

            setupSuggestionServiceAndAdapter(it.getSerializable(EXTRA_SITE_MODEL) as SiteModel, reply)
        }

        return layout
    }

    private fun setupSuggestionServiceAndAdapter(mSite: SiteModel, reply: SuggestionAutoCompleteText) {
        if (!isAdded || mSite == null || !SiteUtils.isAccessedViaWPComRest(mSite)) {
            return
        }
        val mSuggestionServiceConnectionManager = SuggestionServiceConnectionManager(
                activity,
                mSite.siteId
        )
        val mSuggestionAdapter = SuggestionUtils.setupSuggestions(
                mSite, activity,
                mSuggestionServiceConnectionManager
        )
        if (mSuggestionAdapter != null) {
            reply.setAdapter(mSuggestionAdapter)
        }
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
        result.putString(RESULT_REPLY, reply.text.toString())
        result.putInt(RESULT_SELECTION_START, reply.selectionStart)
        result.putInt(RESULT_SELECTION_END, reply.selectionEnd)
        return result
    }

    override fun onViewCreated(controller: CollapseFullScreenDialogController) {
        dialogController = controller
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    companion object {
        const val RESULT_REPLY = "RESULT_REPLY"
        const val RESULT_SELECTION_START = "RESULT_SELECTION_START"
        const val RESULT_SELECTION_END = "RESULT_SELECTION_END"

        private const val EXTRA_REPLY = "EXTRA_REPLY"
        private const val EXTRA_SELECTION_START = "EXTRA_SELECTION_START"
        private const val EXTRA_SELECTION_END = "EXTRA_SELECTION_END"
        private const val EXTRA_SITE_MODEL = "EXTRA_SITE_MODEL"

        fun newBundle(reply: String, selectionStart: Int, selectionEnd: Int, mSite: SiteModel): Bundle {
            val bundle = Bundle()
            bundle.putString(EXTRA_REPLY, reply)
            bundle.putInt(EXTRA_SELECTION_START, selectionStart)
            bundle.putInt(EXTRA_SELECTION_END, selectionEnd)
            bundle.putSerializable(EXTRA_SITE_MODEL, mSite)
            return bundle
        }
    }
}
