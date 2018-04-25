package org.wordpress.android.ui.posts;

import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;

public abstract class BaseYesNoFragmentDialog extends AppCompatDialogFragment {
    private static final String STATE_KEY_TAG = "state_key_tag";
    private static final String STATE_KEY_TITLE = "state_key_title";
    private static final String STATE_KEY_MESSAGE = "state_key_message";
    private static final String STATE_KEY_POSITIVE_BUTTON_LABEL = "state_key_positive_button_label";
    private static final String STATE_KEY_NEGATIVE_BUTTON_LABEL = "state_key_negative_button_label";

    String mTag;
    String mTitle;
    String mMessage;
    String mPositiveButtonLabel;
    String mNegativeButtonLabel;

    public interface BasicYesNoDialogClickInterface {
        void onPositiveClicked(String instanceTag);
        void onNegativeClicked(String instanceTag);
    }

    public void setArgs(String tag,
                        String title,
                        String message,
                        String positiveButtonLabel,
                        String negativeButtonLabel) {
        mTag = tag;
        mTitle = title;
        mMessage = message;
        mPositiveButtonLabel = positiveButtonLabel;
        mNegativeButtonLabel = negativeButtonLabel;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setCancelable(true);
        int style = AppCompatDialogFragment.STYLE_NORMAL, theme = 0;
        setStyle(style, theme);

        if (savedInstanceState != null) {
            mTag = savedInstanceState.getString(STATE_KEY_TAG);
            mTitle = savedInstanceState.getString(STATE_KEY_TITLE);
            mMessage = savedInstanceState.getString(STATE_KEY_MESSAGE);
            mPositiveButtonLabel = savedInstanceState.getString(STATE_KEY_POSITIVE_BUTTON_LABEL);
            mNegativeButtonLabel = savedInstanceState.getString(STATE_KEY_NEGATIVE_BUTTON_LABEL);
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_KEY_TAG, mTag);
        outState.putString(STATE_KEY_TITLE, mTitle);
        outState.putString(STATE_KEY_MESSAGE, mMessage);
        outState.putString(STATE_KEY_POSITIVE_BUTTON_LABEL, mPositiveButtonLabel);
        outState.putString(STATE_KEY_NEGATIVE_BUTTON_LABEL, mNegativeButtonLabel);
        super.onSaveInstanceState(outState);
    }
}
