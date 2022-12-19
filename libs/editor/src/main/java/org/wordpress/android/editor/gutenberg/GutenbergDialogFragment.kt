package org.wordpress.android.editor.gutenberg

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GutenbergDialogFragment : AppCompatDialogFragment() {
    private lateinit var mTag: String
    private lateinit var mMessage: CharSequence
    private var mPositiveButtonLabel: CharSequence? = null
    private var mTitle: CharSequence? = null
    private var mNegativeButtonLabel: CharSequence? = null
    private var mId: Int = 0
    private var dismissedByPositiveButton = false
    private var dismissedByNegativeButton = false

    interface GutenbergDialogPositiveClickInterface {
        fun onGutenbergDialogPositiveClicked(instanceTag: String, id: Int)
    }

    interface GutenbergDialogNegativeClickInterface {
        fun onGutenbergDialogNegativeClicked(instanceTag: String)
    }

    interface GutenbergDialogOnDismissByOutsideTouchInterface {
        fun onDismissByOutsideTouch(instanceTag: String)
    }

    fun initialize(
        tag: String,
        title: CharSequence?,
        message: CharSequence,
        positiveButtonLabel: CharSequence,
        negativeButtonLabel: CharSequence? = null,
        id: Int
    ) {
        mTag = tag
        mTitle = title
        mMessage = message
        mPositiveButtonLabel = positiveButtonLabel
        mNegativeButtonLabel = negativeButtonLabel
        mId = id
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.isCancelable = true
        val theme = 0
        setStyle(STYLE_NORMAL, theme)

        if (savedInstanceState != null) {
            mTag = requireNotNull(savedInstanceState.getString(STATE_KEY_TAG))
            mTitle = savedInstanceState.getCharSequence(STATE_KEY_TITLE)
            mMessage = requireNotNull(savedInstanceState.getCharSequence(STATE_KEY_MESSAGE))
            mPositiveButtonLabel = savedInstanceState.getCharSequence(STATE_KEY_POSITIVE_BUTTON_LABEL)
            mNegativeButtonLabel = savedInstanceState.getCharSequence(STATE_KEY_NEGATIVE_BUTTON_LABEL)
            mId = savedInstanceState.getInt(STATE_KEY_ID)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_KEY_TAG, mTag)
        outState.putCharSequence(STATE_KEY_TITLE, mTitle)
        outState.putCharSequence(STATE_KEY_MESSAGE, mMessage)
        outState.putCharSequence(STATE_KEY_POSITIVE_BUTTON_LABEL, mPositiveButtonLabel)
        outState.putCharSequence(STATE_KEY_NEGATIVE_BUTTON_LABEL, mNegativeButtonLabel)
        outState.putInt(STATE_KEY_ID, mId)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setMessage(mMessage)

        mTitle?.let {
            builder.setTitle(mTitle)
        }

        mPositiveButtonLabel?.let {
            builder.setPositiveButton(mPositiveButtonLabel) { _, _ ->
                dismissedByPositiveButton = true
                (parentFragment as? GutenbergDialogPositiveClickInterface)?.let {
                    it.onGutenbergDialogPositiveClicked(mTag, mId)
                }
            }.setCancelable(true)
        }

        mNegativeButtonLabel?.let {
            builder.setNegativeButton(mNegativeButtonLabel) { _, _ ->
                dismissedByNegativeButton = true
                (parentFragment as? GutenbergDialogNegativeClickInterface)?.let {
                    it.onGutenbergDialogNegativeClicked(mTag)
                }
            }
        }

        return builder.create()
    }

    @Suppress("TooGenericExceptionThrown")
    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parentFragment: Fragment? = parentFragment
        if (parentFragment !is GutenbergDialogPositiveClickInterface) {
            throw RuntimeException("Parent fragment must implement GutenbergDialogPositiveClickInterface")
        }
        if (mNegativeButtonLabel != null && parentFragment !is GutenbergDialogNegativeClickInterface) {
            throw RuntimeException("Parent fragment must implement GutenbergDialogNegativeClickInterface")
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Only handle the event if it wasn't triggered by a button
        if (!dismissedByPositiveButton && !dismissedByNegativeButton) {
            (parentFragment as? GutenbergDialogOnDismissByOutsideTouchInterface)?.let {
                it.onDismissByOutsideTouch(mTag)
            }
        }
        super.onDismiss(dialog)
    }

    companion object {
        private const val STATE_KEY_TAG = "state_key_tag"
        private const val STATE_KEY_TITLE = "state_key_title"
        private const val STATE_KEY_MESSAGE = "state_key_message"
        private const val STATE_KEY_POSITIVE_BUTTON_LABEL = "state_key_positive_button_label"
        private const val STATE_KEY_NEGATIVE_BUTTON_LABEL = "state_key_negative_button_label"
        private const val STATE_KEY_ID = "state_key_id"
    }
}
