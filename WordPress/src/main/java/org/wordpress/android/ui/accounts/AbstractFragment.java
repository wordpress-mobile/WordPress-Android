package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.WPActivityUtils;

/**
 * A fragment representing a single step in a wizard. The fragment shows a dummy title indicating
 * the page number, along with some dummy text.
 */
public abstract class AbstractFragment extends Fragment {
    protected static RequestQueue requestQueue;
    protected static RestClientUtils mRestClientUtils;
    protected ConnectivityManager mSystemService;
    protected boolean mPasswordVisible;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSystemService = (ConnectivityManager) getActivity().getApplicationContext().
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(getActivity());
        }
    }

    protected RestClientUtils getRestClientUtils() {
        if (mRestClientUtils == null) {
            mRestClientUtils = new RestClientUtils(getContext(), requestQueue, null, null);
        }
        return mRestClientUtils;
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
            View view = getActivity().getCurrentFocus();
            if (view != null) WPActivityUtils.hideKeyboard(view);

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
                    passwordVisibility.setImageResource(R.drawable.dashicon_eye_open);
                    passwordVisibility.setColorFilter(v.getContext().getResources().getColor(R.color.nux_eye_icon_color_open));
                    passwordEditText.setTransformationMethod(null);
                } else {
                    passwordVisibility.setImageResource(R.drawable.dashicon_eye_closed);
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
                R.drawable.noticon_alert_big, getString(R.string.nux_tap_continue));
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

    protected int getErrorMessageForErrorCode(String errorCode) {
        if (errorCode.equals("username_only_lowercase_letters_and_numbers")) {
            return R.string.username_only_lowercase_letters_and_numbers;
        }
        if (errorCode.equals("username_required")) {
            return R.string.username_required;
        }
        if (errorCode.equals("username_not_allowed")) {
            return R.string.username_not_allowed;
        }
        if (errorCode.equals("email_cant_be_used_to_signup")) {
            return R.string.email_cant_be_used_to_signup;
        }
        if (errorCode.equals("username_must_be_at_least_four_characters")) {
            return R.string.username_must_be_at_least_four_characters;
        }
        if (errorCode.equals("username_contains_invalid_characters")) {
            return R.string.username_contains_invalid_characters;
        }
        if (errorCode.equals("username_must_include_letters")) {
            return R.string.username_must_include_letters;
        }
        if (errorCode.equals("email_invalid")) {
            return R.string.email_invalid;
        }
        if (errorCode.equals("email_not_allowed")) {
            return R.string.email_not_allowed;
        }
        if (errorCode.equals("username_exists")) {
            return R.string.username_exists;
        }
        if (errorCode.equals("email_exists")) {
            return R.string.email_exists;
        }
        if (errorCode.equals("username_reserved_but_may_be_available")) {
            return R.string.username_reserved_but_may_be_available;
        }
        if (errorCode.equals("email_reserved")) {
            return R.string.email_reserved;
        }
        if (errorCode.equals("blog_name_required")) {
            return R.string.blog_name_required;
        }
        if (errorCode.equals("blog_name_not_allowed")) {
            return R.string.blog_name_not_allowed;
        }
        if (errorCode.equals("blog_name_must_be_at_least_four_characters")) {
            return R.string.blog_name_must_be_at_least_four_characters;
        }
        if (errorCode.equals("blog_name_must_be_less_than_sixty_four_characters")) {
            return R.string.blog_name_must_be_less_than_sixty_four_characters;
        }
        if (errorCode.equals("blog_name_contains_invalid_characters")) {
            return R.string.blog_name_contains_invalid_characters;
        }
        if (errorCode.equals("blog_name_cant_be_used")) {
            return R.string.blog_name_cant_be_used;
        }
        if (errorCode.equals("blog_name_only_lowercase_letters_and_numbers")) {
            return R.string.blog_name_only_lowercase_letters_and_numbers;
        }
        if (errorCode.equals("blog_name_must_include_letters")) {
            return R.string.blog_name_must_include_letters;
        }
        if (errorCode.equals("blog_name_exists")) {
            return R.string.blog_name_exists;
        }
        if (errorCode.equals("blog_name_reserved")) {
            return R.string.blog_name_reserved;
        }
        if (errorCode.equals("blog_name_reserved_but_may_be_available")) {
            return R.string.blog_name_reserved_but_may_be_available;
        }
        if (errorCode.equals("password_invalid")) {
            return R.string.password_invalid;
        }
        if (errorCode.equals("blog_name_invalid")) {
            return R.string.blog_name_invalid;
        }
        if (errorCode.equals("blog_title_invalid")) {
            return R.string.blog_title_invalid;
        }
        if (errorCode.equals("username_invalid")) {
            return R.string.username_invalid;
        }
        return 0;
    }

    protected enum ErrorType {USERNAME, PASSWORD, SITE_URL, EMAIL, TITLE, UNDEFINED}

    public class ErrorListener implements RestRequest.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {
            String message = null;
            int messageId;
            AppLog.e(T.NUX, error);
            if (error.networkResponse != null && error.networkResponse.data != null) {
                AppLog.e(T.NUX, String.format("Error message: %s", new String(error.networkResponse.data)));
                String jsonString = new String(error.networkResponse.data);
                try {
                    JSONObject errorObj = new JSONObject(jsonString);
                    messageId = getErrorMessageForErrorCode((String) errorObj.get("error"));
                    if (messageId == 0) {
                        // Not one of our common errors. Show the error message from the server.
                        message = (String) errorObj.get("message");
                    }
                } catch (JSONException e) {
                    AppLog.e(T.NUX, e);
                    messageId = R.string.error_generic;
                }
            } else {
                if (error.getMessage() != null) {
                    if (error.getMessage().contains("Limit reached")) {
                        messageId = R.string.limit_reached;
                    } else {
                        messageId = R.string.error_generic;
                    }
                } else {
                    messageId = R.string.error_generic;
                }
            }
            endProgress();
            if (messageId == 0) {
                showError(message);
            } else {
                showError(messageId);
            }
        }
    }
}
