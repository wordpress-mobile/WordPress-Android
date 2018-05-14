package org.wordpress.android.ui.posts

import android.os.Bundle
import android.support.v7.app.AlertDialog.Builder

class BasicYesNoCancelFragmentDialog : BasicYesNoFragmentDialog() {
    internal var mCancelButtonLabel: String? = null

    fun initialize(
        tag: String,
        title: String,
        message: String,
        positiveButtonLabel: String,
        negativeButtonLabel: String,
        neutralButtonLabel: String
    ) {
        initialize(tag, title, message, positiveButtonLabel, negativeButtonLabel)
        mCancelButtonLabel = neutralButtonLabel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mCancelButtonLabel = savedInstanceState.getString(STATE_KEY_NEUTRAL_BUTTON_LABEL)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_KEY_NEUTRAL_BUTTON_LABEL, mCancelButtonLabel)
        super.onSaveInstanceState(outState)
    }

    override fun initBuilder(builder: Builder) {
        super.initBuilder(builder)
        builder.setNeutralButton(mCancelButtonLabel) { dialog, which ->
            // no-op - dialog is automatically dismissed
        }
    }

    companion object {
        private val STATE_KEY_NEUTRAL_BUTTON_LABEL = "state_key_neutral_button_label"
    }
}
