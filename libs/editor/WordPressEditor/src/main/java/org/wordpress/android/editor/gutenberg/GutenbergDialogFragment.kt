package org.wordpress.android.editor.gutenberg

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.Serializable

class GutenbergDialogFragment<T : Serializable?>() : AppCompatDialogFragment() {
    private lateinit var mTag: String
    private var mMessage: CharSequence? = null
    private lateinit var mPositiveButtonLabel: CharSequence
    private var mTitle: CharSequence? = null
    private var mNegativeButtonLabel: CharSequence? = null
    private var mDataFromGutenberg: T? = null
    private var dismissedByPositiveButton = false
    private var dismissedByNegativeButton = false

    interface GutenbergDialogPositiveClickInterface<T> {
        fun onGutenbergDialogPositiveClicked(instanceTag: String, dataFromGutenberg: Any?)
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
        message: CharSequence?,
        positiveButtonLabel: CharSequence,
        negativeButtonLabel: CharSequence? = null,
        dataFromGutenberg: T
    ) {
        mTag = tag
        mTitle = title
        mMessage = message
        mPositiveButtonLabel = positiveButtonLabel
        mNegativeButtonLabel = negativeButtonLabel
        mDataFromGutenberg = dataFromGutenberg
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.isCancelable = true
        val theme = 0
        setStyle(STYLE_NORMAL, theme)

        if (savedInstanceState != null) {
            mTag = requireNotNull(savedInstanceState.getString(STATE_KEY_TAG))
            mTitle = savedInstanceState.getCharSequence(STATE_KEY_TITLE)
            mMessage = savedInstanceState.getCharSequence(STATE_KEY_MESSAGE)
            mPositiveButtonLabel = requireNotNull(savedInstanceState.getCharSequence(STATE_KEY_POSITIVE_BUTTON_LABEL))
            mNegativeButtonLabel = savedInstanceState.getCharSequence(STATE_KEY_NEGATIVE_BUTTON_LABEL)
            mDataFromGutenberg = savedInstanceState.getSerializable(STATE_KEY_DATA_FROM_GUTENBERG) as T?
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_KEY_TAG, mTag)
        outState.putCharSequence(STATE_KEY_TITLE, mTitle)
        outState.putCharSequence(STATE_KEY_MESSAGE, mMessage)
        outState.putCharSequence(STATE_KEY_POSITIVE_BUTTON_LABEL, mPositiveButtonLabel)
        outState.putCharSequence(STATE_KEY_NEGATIVE_BUTTON_LABEL, mNegativeButtonLabel)
        outState.putSerializable(STATE_KEY_DATA_FROM_GUTENBERG, mDataFromGutenberg)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireActivity())

        mTitle?.let {
            builder.setTitle(mTitle)
        }

        mMessage?.let {
            builder.setMessage(mMessage)
        }

        mPositiveButtonLabel?.let {
            builder.setPositiveButton(mPositiveButtonLabel) { _, _ ->
                dismissedByPositiveButton = true
                (parentFragment as? GutenbergDialogPositiveClickInterface<*>)?.let {
                    it.onGutenbergDialogPositiveClicked(mTag, mDataFromGutenberg)
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parentFragment: Fragment? = parentFragment
        if (parentFragment !is GutenbergDialogPositiveClickInterface<*>) {
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
        private const val STATE_KEY_DATA_FROM_GUTENBERG = "state_key_data_from_gutenberg"
    }
}
