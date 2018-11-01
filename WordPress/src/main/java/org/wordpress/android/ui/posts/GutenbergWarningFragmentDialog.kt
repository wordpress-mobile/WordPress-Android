package org.wordpress.android.ui.posts

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import org.wordpress.android.R
import org.wordpress.android.widgets.WPTextView

/**
 * Basic dialog fragment with support for 1,2 or 3 buttons.
 */
class GutenbergWarningFragmentDialog : AppCompatDialogFragment() {
    private lateinit var mTag: String
    private var mGutenbergRemotePostId: Long = 0
    private var mIsPage: Boolean = false

    interface GutenbergWarningDialogClickInterface {
        fun onGutenbergWarningDialogEditPostClicked(instanceTag: String, gutenbergRemotePostId: Long)
        fun onGutenbergWarningDialogCancelClicked(instanceTag: String, gutenbergRemotePostId: Long)
        fun onGutenbergWarningDialogLearnMoreLinkClicked(instanceTag: String, gutenbergRemotePostId: Long)
        fun onGutenbergWarningDialogDontShowAgainClicked(
            instanceTag: String,
            gutenbergRemotePostId: Long,
            checked: Boolean
        )
    }

    fun initialize(
        tag: String,
        isPage: Boolean,
        gutenbergRemotePostId: Long
    ) {
        mTag = tag
        mIsPage = isPage
        mGutenbergRemotePostId = gutenbergRemotePostId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.isCancelable = false
        val theme = 0
        setStyle(AppCompatDialogFragment.STYLE_NORMAL, theme)

        if (savedInstanceState != null) {
            mTag = savedInstanceState.getString(STATE_KEY_TAG)
            mIsPage = savedInstanceState.getBoolean(STATE_KEY_IS_PAGE)
            mGutenbergRemotePostId = savedInstanceState.getLong(STATE_KEY_GUTENBERG_REMOTE_POST_ID)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_KEY_TAG, mTag)
        outState.putBoolean(STATE_KEY_IS_PAGE, mIsPage)
        outState.putLong(STATE_KEY_GUTENBERG_REMOTE_POST_ID, mGutenbergRemotePostId)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.gutenberg_warning_dialog, container, false)
        initializeView(view)
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        return dialog
    }

    private fun initializeView(view: View) {
        val dialogTitle = view.findViewById<WPTextView>(R.id.gutenberg_warning_dialog_title)
        dialogTitle.text = getString(R.string.dialog_gutenberg_compatibility_title)

        val dialogMessage = view.findViewById<WPTextView>(R.id.gutenberg_warning_dialog_description)
        val messageText = if (mIsPage)
            getString(R.string.dialog_gutenberg_compatibility_message_page)
        else
            getString(R.string.dialog_gutenberg_compatibility_message)
        dialogMessage.text = messageText

        val link = view.findViewById<WPTextView>(R.id.gutenberg_warning_dialog_link)
        link.text = getString(R.string.dialog_gutenberg_compatibility_learn_more)
        link.setOnClickListener({
            (activity as GutenbergWarningDialogClickInterface)
                    .onGutenbergWarningDialogLearnMoreLinkClicked(mTag, mGutenbergRemotePostId) })

        val positiveButtonLabel = if (mIsPage)
            getString(R.string.dialog_gutenberg_compatibility_yes_edit_page)
        else
            getString(R.string.dialog_gutenberg_compatibility_yes_edit_post)
        val buttonPositive = view.findViewById<Button>(R.id.gutenberg_warning_dialog_button_positive)
        buttonPositive.text = positiveButtonLabel
        buttonPositive.setOnClickListener({
                (activity as GutenbergWarningDialogClickInterface)
                        .onGutenbergWarningDialogEditPostClicked(mTag, mGutenbergRemotePostId)
            this.dismiss()
        })

        val buttonNegative = view.findViewById<Button>(R.id.gutenberg_warning_dialog_button_negative)
        buttonNegative.visibility = View.VISIBLE
        buttonNegative.text = getString(R.string.dialog_gutenberg_compatibility_no_go_back)
        buttonNegative.setOnClickListener({
                (activity as GutenbergWarningDialogClickInterface)
                        .onGutenbergWarningDialogCancelClicked(mTag, mGutenbergRemotePostId)
            this.dismiss()
        })

        val dontShowAnymore = view.findViewById<CheckBox>(R.id.gutenberg_warning_dialog_dont_show_anymore)
        dontShowAnymore.setOnClickListener({
                (activity as GutenbergWarningDialogClickInterface)
                        .onGutenbergWarningDialogDontShowAgainClicked(
                                mTag, mGutenbergRemotePostId, dontShowAnymore.isChecked)
        })
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (activity !is GutenbergWarningDialogClickInterface) {
            throw RuntimeException("Hosting activity must implement GutenbergWarningDialogClickInterface")
        }
    }

    companion object {
        private const val STATE_KEY_TAG = "state_key_tag"
        private const val STATE_KEY_IS_PAGE = "state_key_is_page"
        private const val STATE_KEY_GUTENBERG_REMOTE_POST_ID = "state_key_gb_remote_post_id"
    }
}
