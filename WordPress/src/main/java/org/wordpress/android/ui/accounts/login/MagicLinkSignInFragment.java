package org.wordpress.android.ui.accounts.login;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.SignInFragment;
import org.wordpress.android.util.EditTextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MagicLinkSignInFragment extends SignInFragment {
    public interface OnMagicLinkRequestListener {
        void onMagicLinkRequestSuccess();
    }

    private OnMagicLinkRequestListener mListener;

    public MagicLinkSignInFragment() {
        super();
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

        configureMagicLinkUI();
        mUsernameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (didPressNextKey(actionId, event) && !isEnterPasswordMode()) {
                    signIn();
                    return true;
                } else {
                    return false;
                }
            }
        });

        return view;
    }

    private boolean didPressNextKey(int actionId, KeyEvent event) {
        return actionId == EditorInfo.IME_ACTION_NEXT || event != null && (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_NAVIGATE_NEXT);
    }

    private void configureMagicLinkUI() {
        showDotComSignInForm();
        mPasswordLayout.setVisibility(View.GONE);
        mForgotPassword.setVisibility(View.GONE);
        mSignInButton.setText(getString(R.string.button_next));
    }

    protected void showSelfHostedSignInForm(){
        super.showSelfHostedSignInForm();
        showPasswordField();
    }

    @Override
    protected void toggleSignInMode(){
        if (mUrlButtonLayout.getVisibility() == View.VISIBLE) {
            configureMagicLinkUI();
            mSelfHosted = false;
        } else {
            showSelfHostedSignInForm();
            mSelfHosted = true;
        }
    }

    private boolean isEnterPasswordMode() {
        return mPasswordLayout.getVisibility() == View.VISIBLE;
    }

    @Override
    protected void signIn() {
        if (mSelfHosted || isEnterPasswordMode()) {
            super.signIn();
        } else {
            if (isUsernameEmail()) {
                requestWPComEmailCheck();
            } else {
                showPasswordFieldAndFocus();
            }
        }
    }

    private void showPasswordFieldAndFocus() {
        showPasswordField();
        mPasswordEditText.requestFocus();
        mSignInButton.setText(getString(R.string.sign_in));
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mPasswordEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void showPasswordField() {
        mPasswordLayout.setVisibility(View.VISIBLE);
        mForgotPassword.setVisibility(View.VISIBLE);
        if (!mSelfHosted) {
            mPasswordEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }
    }

    private void requestWPComEmailCheck() {
        WordPress.getRestClientUtilsV0().isAvailable(mUsername, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                showPasswordFieldAndFocus();
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String string = error.getMessage();
                try {
                    JSONObject jsonResponse = new JSONObject(string);
                    String errorReason = jsonResponse.getString("error");
                    if (errorReason != null && errorReason.equals("taken")) {
                        mListener.onMagicLinkRequestSuccess();
                    } else {
                        showPasswordFieldAndFocus();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    showPasswordFieldAndFocus();
                }
            }
        });
    }

    private boolean isUsernameEmail() {
        mUsername = EditTextUtils.getText(mUsernameEditText).trim();
        Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(mUsername);

        return matcher.find() && mUsername.length() <= 100;
    }
}
