package org.wordpress.android.ui.accounts.login;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import org.wordpress.android.R;
import org.wordpress.android.ui.accounts.SignInFragment;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.WPActivityUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewSignInFragment extends SignInFragment {
    public interface OnMagicLinkRequestListener {
        void onMagicLinkRequestSuccess();
    }

    private OnMagicLinkRequestListener mListener;
    private boolean mIsMagicLink;

    public NewSignInFragment() {
        super();
        mIsMagicLink = true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMagicLinkRequestListener) {
            mListener = (OnMagicLinkRequestListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnMagicLinkRequestListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (mIsMagicLink) {
            hideForMagicLink();
        } else {
            hideForNonMagicLink();
        }

        return view;
    }

    public void setIsMagicLink(boolean isMagicLink) {
        mIsMagicLink = isMagicLink;
    }

    private void hideForMagicLink() {
        mUrlButtonLayout.setVisibility(View.GONE);
        mPasswordLayout.setVisibility(View.GONE);
        mForgotPassword.setVisibility(View.GONE);
        mSignInButton.setText(getString(R.string.button_next));
    }

    private void hideForNonMagicLink() {
        mUrlButtonLayout.setVisibility(View.GONE);
        mCreateAccountButton.setVisibility(View.GONE);
        mAddSelfHostedButton.setVisibility(View.GONE);
        mPasswordEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    @Override
    protected void toggleSignInMode() {
        super.toggleSignInMode();

        mIsMagicLink = !mSelfHosted;
        if (mSelfHosted) {
            mPasswordLayout.setVisibility(View.VISIBLE);
            mForgotPassword.setVisibility(View.VISIBLE);
        } else {
            mPasswordLayout.setVisibility(View.GONE);
            mForgotPassword.setVisibility(View.GONE);
        }
    }

    @Override
    protected void signIn() {
        if (mIsMagicLink) {
            boolean isValidWPComEmail = true;
            if (isValidWPComEmail) { // OK RESPONSE
                mListener.onMagicLinkRequestSuccess();
            } else { // ERROR RESPONSE
                mPasswordLayout.setVisibility(View.VISIBLE);
                mForgotPassword.setVisibility(View.VISIBLE);
            }
        } else {
            super.signIn();
        }
    }

    private boolean isUsernameEmail() {
        mUsername = EditTextUtils.getText(mUsernameEditText).trim();

        Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(mUsername);

        return matcher.find() && mUsername.length() <= 100;
    }
}
