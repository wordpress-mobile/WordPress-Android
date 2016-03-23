package org.wordpress.android.ui.accounts.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.ui.accounts.SignInFragment;

public class NewSignInFragment extends SignInFragment {
    public interface OnEmailCheckListener {
        void onEmailChecked(boolean isWPCom);
    }

    public NewSignInFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        hideIrrelevantViews();

        return view;
    }

    private void hideIrrelevantViews() {
        mUrlButtonLayout.setVisibility(View.GONE);
        mPasswordLayout.setVisibility(View.GONE);
        mForgotPassword.setVisibility(View.GONE);
    }

    @Override
    protected void toggleSignInMode() {
        super.toggleSignInMode();

        if (mSelfHosted) {
            mPasswordLayout.setVisibility(View.VISIBLE);
            mForgotPassword.setVisibility(View.VISIBLE);
        } else {
            mPasswordLayout.setVisibility(View.GONE);
            mForgotPassword.setVisibility(View.GONE);
        }
    }
}
