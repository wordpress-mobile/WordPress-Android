package org.wordpress.android.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.CollapseFullScreenDialogContent
import org.wordpress.android.ui.CollapseFullScreenDialogFragment.CollapseFullScreenDialogController
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager
import org.wordpress.android.ui.suggestion.util.SuggestionUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.widgets.SuggestionAutoCompleteText
import javax.inject.Inject

class CommentFullScreenDialogFragment : Fragment(), CollapseFullScreenDialogContent {
    @Inject lateinit var viewModel: CommentFullScreenDialogViewModel
    @Inject lateinit var siteStore: SiteStore
    private lateinit var dialogController: CollapseFullScreenDialogController
    private lateinit var reply: SuggestionAutoCompleteText
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

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
                dialogController.setConfirmEnabled(!TextUtils.isEmpty(s.toString().trim()))
            }
        })

        viewModel.onKeyboardOpened.observe(viewLifecycleOwner, Observer {
            it?.applyIfNotHandled {
                coroutineScope.launch {
                    val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showSoftInput(reply, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        })

        arguments?.let {
            reply.setText(it.getString(EXTRA_REPLY))
            reply.setSelection(it.getInt(EXTRA_SELECTION_START), it.getInt(EXTRA_SELECTION_END))
            viewModel.init()

            // Allow @username suggestion in full screen comment Editor on the Reader,
            // but only on sites in the siteStore (i.e: current user's site).
            // No suggestion is available for external sites that the user follows in the Reader.
            val siteModel: SiteModel? = siteStore.getSiteBySiteId(it.getLong(EXTRA_SITE_ID))
            if (siteModel != null) {
                setupSuggestionServiceAndAdapter(siteModel)
            }
        }

        return layout
    }

    private fun setupSuggestionServiceAndAdapter(site: SiteModel) {
        if (!isAdded || !SiteUtils.isAccessedViaWPComRest(site)) {
            return
        }
        val suggestionServiceConnectionManager = SuggestionServiceConnectionManager(
                activity,
                site.siteId
        )
        val suggestionAdapter = SuggestionUtils.setupSuggestions(
                site, activity,
                suggestionServiceConnectionManager,
                false
        )
        if (suggestionAdapter != null) {
            this.reply.setAdapter(suggestionAdapter)
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

    override fun onAttach(context: Context) {
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
        private const val EXTRA_SITE_ID = "EXTRA_SITE_ID"

        fun newBundle(reply: String, selectionStart: Int, selectionEnd: Int, siteId: Long): Bundle {
            val bundle = Bundle()
            bundle.putString(EXTRA_REPLY, reply)
            bundle.putInt(EXTRA_SELECTION_START, selectionStart)
            bundle.putInt(EXTRA_SELECTION_END, selectionEnd)
            bundle.putLong(EXTRA_SITE_ID, siteId)
            return bundle
        }
    }
}
