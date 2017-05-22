package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.util.WPActivityUtils;

/**
 * A fragment representing a single step in a wizard. The fragment shows a dummy title indicating
 * the page number, along with some dummy text.
 */
public abstract class AbstractFragment extends Fragment {
    protected ConnectivityManager mSystemService;
    protected boolean mPasswordVisible;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSystemService = (ConnectivityManager) getActivity().getApplicationContext().
                getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    protected void startProgress(String message) {
    }

    protected void updateProgress(String message) {
    }

    protected void endProgress() {
    }

    protected abstract void onDoneAction();

    protected abstract boolean isUserDataValid();

    protected boolean onDoneEvent(int actionId, KeyEvent event) {
        if (didPressEnterKey(actionId, event)) {
            if (!isUserDataValid()) {
                return true;
            }

            // hide keyboard before calling the done action
            if (getActivity() != null) {
                WPActivityUtils.hideKeyboard(getActivity().getCurrentFocus());
            }

            // call child action
            onDoneAction();
            return true;
        }
        return false;
    }

    protected boolean didPressNextKey(int actionId, KeyEvent event) {
        return actionId == EditorInfo.IME_ACTION_NEXT || event != null && (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_NAVIGATE_NEXT);
    }

    protected boolean didPressEnterKey(int actionId, KeyEvent event) {
        return actionId == EditorInfo.IME_ACTION_DONE || event != null && (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
    }

    protected void initPasswordVisibilityButton(View rootView, final EditText passwordEditText) {
        final ImageView passwordVisibility = (ImageView) rootView.findViewById(R.id.password_visibility);
        if (passwordVisibility == null) {
            return;
        }
        passwordVisibility.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPasswordVisible = !mPasswordVisible;
                if (mPasswordVisible) {
                    passwordVisibility.setImageResource(R.drawable.ic_visible_on_black_24dp);
                    passwordVisibility.setColorFilter(v.getContext().getResources().getColor(R.color.nux_eye_icon_color_open));
                    passwordEditText.setTransformationMethod(null);
                } else {
                    passwordVisibility.setImageResource(R.drawable.ic_visible_off_black_24dp);
                    passwordVisibility.setColorFilter(v.getContext().getResources().getColor(R.color.nux_eye_icon_color_closed));
                    passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                passwordEditText.setSelection(passwordEditText.length());
            }
        });
    }

    protected boolean specificShowError(int messageId) {
        return false;
    }

    protected void showError(int messageId) {
        if (!isAdded()) {
            return;
        }
        if (specificShowError(messageId)) {
            return;
        }
        // Failback if it's not a specific error
        showError(getString(messageId));
    }

    protected void showError(String message) {
        if (!isAdded()) {
            return;
        }
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SignInDialogFragment nuxAlert = SignInDialogFragment.newInstance(getString(R.string.error), message,
                R.drawable.ic_notice_white_64dp, getString(R.string.nux_tap_continue));
        ft.add(nuxAlert, "alert");
        ft.commitAllowingStateLoss();
    }

    protected ErrorType getErrorType(int messageId) {
        if (messageId == R.string.username_only_lowercase_letters_and_numbers ||
                messageId == R.string.username_required || messageId == R.string.username_not_allowed ||
                messageId == R.string.username_must_be_at_least_four_characters ||
                messageId == R.string.username_contains_invalid_characters ||
                messageId == R.string.username_must_include_letters || messageId == R.string.username_exists ||
                messageId == R.string.username_reserved_but_may_be_available ||
                messageId == R.string.username_invalid) {
            return ErrorType.USERNAME;
        } else if (messageId == R.string.password_invalid) {
            return ErrorType.PASSWORD;
        } else if (messageId == R.string.email_cant_be_used_to_signup || messageId == R.string.email_invalid ||
                messageId == R.string.email_not_allowed || messageId == R.string.email_exists ||
                messageId == R.string.email_reserved) {
            return ErrorType.EMAIL;
        } else if (messageId == R.string.blog_name_required || messageId == R.string.blog_name_not_allowed ||
                messageId == R.string.blog_name_must_be_at_least_four_characters ||
                messageId == R.string.blog_name_must_be_less_than_sixty_four_characters ||
                messageId == R.string.blog_name_contains_invalid_characters ||
                messageId == R.string.blog_name_cant_be_used ||
                messageId == R.string.blog_name_only_lowercase_letters_and_numbers ||
                messageId == R.string.blog_name_must_include_letters || messageId == R.string.blog_name_exists ||
                messageId == R.string.blog_name_reserved ||
                messageId == R.string.blog_name_reserved_but_may_be_available ||
                messageId == R.string.blog_name_invalid) {
            return ErrorType.SITE_URL;
        } else if (messageId == R.string.blog_title_invalid) {
            return ErrorType.TITLE;
        }
        return ErrorType.UNDEFINED;
    }

    protected enum ErrorType {USERNAME, PASSWORD, SITE_URL, EMAIL, TITLE, UNDEFINED}

    protected void lowerCaseEditable(Editable editable) {
        // Convert editable content to lowercase
        String lowerCase = editable.toString().toLowerCase();
        if (!lowerCase.equals(editable.toString())) {
            editable.replace(0, editable.length(), lowerCase);
        }
    }

}
