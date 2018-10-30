package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPTextView

class GutenbergWarningFragmentDialog : BasicFragmentDialog() {
    interface GutenbergWarningDialogLearnMoreLinkClickInterface {
        fun onLearnMoreLinkClicked(instanceTag: String)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.gutenberg_warning_dialog, container, false)
        initializeView(view)
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        willUseCustomLayout = true
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        return dialog
    }

    private fun initializeView(view: View) {
        val dialogTitle = view.findViewById<WPTextView>(R.id.gutenberg_warning_dialog_title)
        dialogTitle.text = mTitle

        val dialogMessage = view.findViewById<WPTextView>(R.id.gutenberg_warning_dialog_description)
        dialogMessage.text = mMessage

        if (activity is GutenbergWarningDialogLearnMoreLinkClickInterface) {
            val link = view.findViewById<WPTextView>(R.id.gutenberg_warning_dialog_link)
            link.text = getString(R.string.dialog_gutenberg_compatibility_learn_more)
            link.setOnClickListener({
                (activity as GutenbergWarningDialogLearnMoreLinkClickInterface).onLearnMoreLinkClicked(mTag) })
        }

        val buttonPositive = view.findViewById<Button>(R.id.gutenberg_warning_dialog_button_positive)
        buttonPositive.text = mPositiveButtonLabel
        buttonPositive.setOnClickListener({
            if (activity is BasicDialogPositiveClickInterface) {
                (activity as BasicDialogPositiveClickInterface).onPositiveClicked(mTag, mExtras)
            }
            this.dismiss()
        })

        val buttonNegative = view.findViewById<Button>(R.id.gutenberg_warning_dialog_button_negative)
        buttonNegative.visibility = View.VISIBLE
        buttonNegative.text = mNegativeButtonLabel
        buttonNegative.setOnClickListener({
            if (activity is BasicDialogNegativeClickInterface) {
                (activity as BasicDialogNegativeClickInterface).onNegativeClicked(mTag, mExtras)
            }
            this.dismiss()
        })
    }
}
