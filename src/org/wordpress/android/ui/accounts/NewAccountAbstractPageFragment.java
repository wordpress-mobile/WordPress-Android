package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.WPRestClient;

/**
 * A fragment representing a single step in a wizard. The fragment shows a dummy title indicating
 * the page number, along with some dummy text.
 *
 */
public abstract class NewAccountAbstractPageFragment extends SherlockFragment {
    protected ConnectivityManager mSystemService;
    protected static RequestQueue requestQueue = null;
    protected static WPRestClient restClient = null;
    protected boolean mPasswordVisible;

    protected enum ErrorType {USERNAME, PASSWORD, SITE_URL, EMAIL, TITLE, UNDEFINED}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSystemService = (ConnectivityManager) getActivity().getApplicationContext().
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (requestQueue == null)
            requestQueue = Volley.newRequestQueue(getActivity());
        if (restClient == null)
            restClient = new WPRestClient(requestQueue, null);
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
        if (actionId == EditorInfo.IME_ACTION_DONE || event != null && (event.getAction() == KeyEvent.ACTION_DOWN &&
                                                                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            if (!isUserDataValid()) {
                return true;
            }

            // hide keyboard before calling the done action
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            View view = getActivity().getCurrentFocus();
            if (view != null) {
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }

            // call child action
            onDoneAction();
            return true;
        }
        return false;
    }

    protected void initPasswordVisibilityButton(View rootView, final EditText passwordEditText) {
        final ImageView passwordVisibility = (ImageView) rootView.findViewById(R.id.password_visibility);
        if (passwordVisibility == null) {
            return ;
        }
        passwordVisibility.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPasswordVisible = !mPasswordVisible;
                if (mPasswordVisible) {
                    passwordVisibility.setImageResource(R.drawable.dashicon_eye_open);
                    passwordEditText.setTransformationMethod(null);
                } else {
                    passwordVisibility.setImageResource(R.drawable.dashicon_eye_closed);
                    passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                passwordEditText.setSelection(passwordEditText.length());
            }
        }
        );
    }

    protected class ErrorListener implements RestRequest.ErrorListener {
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
                    if (messageId == 0) { // Not one of our common errors. Show the error message
                        // from the server.
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

    protected boolean specificShowError(int messageId) {
        return false;
    }

    protected boolean hasActivity() {
        return (getActivity() != null && !isRemoving());
    }

    protected void showError(int messageId) {
        if (!hasActivity()) {
            return ;
        }
        if (specificShowError(messageId)) {
            return ;
        }
        // Failback if it's not a specific error
        showError(getString(messageId));
    }

    protected void showError(String message) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        NUXDialogFragment nuxAlert = NUXDialogFragment.newInstance(getString(R.string.error), message, getString(
                R.string.nux_tap_continue), R.drawable.nux_icon_alert);
        ft.add(nuxAlert,  "alert");
        ft.commitAllowingStateLoss();
    }

    protected ErrorType getErrorType(int messageId) {
        switch (messageId) {
            case R.string.username_only_lowercase_letters_and_numbers:
            case R.string.username_required:
            case R.string.username_not_allowed:
            case R.string.username_must_be_at_least_four_characters:
            case R.string.username_contains_invalid_characters:
            case R.string.username_must_include_letters:
            case R.string.username_exists:
            case R.string.username_reserved_but_may_be_available:
            case R.string.username_invalid:
                return ErrorType.USERNAME;
            case R.string.password_invalid:
                return ErrorType.PASSWORD;
            case R.string.email_cant_be_used_to_signup:
            case R.string.email_invalid:
            case R.string.email_not_allowed:
            case R.string.email_exists:
            case R.string.email_reserved:
                return ErrorType.EMAIL;
            case R.string.blog_name_required:
            case R.string.blog_name_not_allowed:
            case R.string.blog_name_must_be_at_least_four_characters:
            case R.string.blog_name_must_be_less_than_sixty_four_characters:
            case R.string.blog_name_contains_invalid_characters:
            case R.string.blog_name_cant_be_used:
            case R.string.blog_name_only_lowercase_letters_and_numbers:
            case R.string.blog_name_must_include_letters:
            case R.string.blog_name_exists:
            case R.string.blog_name_reserved:
            case R.string.blog_name_reserved_but_may_be_available:
            case R.string.blog_name_invalid:
                return ErrorType.SITE_URL;
            case R.string.blog_title_invalid:
                return ErrorType.TITLE;
        }
        return ErrorType.UNDEFINED;
    }

    protected int getErrorMessageForErrorCode(String errorCode) {
        if (errorCode.equals("username_only_lowercase_letters_and_numbers"))
            return R.string.username_only_lowercase_letters_and_numbers;
        if (errorCode.equals("username_required"))
            return R.string.username_required;
        if (errorCode.equals("username_not_allowed"))
            return R.string.username_not_allowed;
        if (errorCode.equals("email_cant_be_used_to_signup"))
            return R.string.email_cant_be_used_to_signup;
        if (errorCode.equals("username_must_be_at_least_four_characters"))
            return R.string.username_must_be_at_least_four_characters;
        if (errorCode.equals("username_contains_invalid_characters"))
            return R.string.username_contains_invalid_characters;
        if (errorCode.equals("username_must_include_letters"))
            return R.string.username_must_include_letters;
        if (errorCode.equals("email_invalid"))
            return R.string.email_invalid;
        if (errorCode.equals("email_not_allowed"))
            return R.string.email_not_allowed;
        if (errorCode.equals("username_exists"))
            return R.string.username_exists;
        if (errorCode.equals("email_exists"))
            return R.string.email_exists;
        if (errorCode.equals("username_reserved_but_may_be_available"))
            return R.string.username_reserved_but_may_be_available;
        if (errorCode.equals("email_reserved"))
            return R.string.email_reserved;
        if (errorCode.equals("blog_name_required"))
            return R.string.blog_name_required;
        if (errorCode.equals("blog_name_not_allowed"))
            return R.string.blog_name_not_allowed;
        if (errorCode.equals("blog_name_must_be_at_least_four_characters"))
            return R.string.blog_name_must_be_at_least_four_characters;
        if (errorCode.equals("blog_name_must_be_less_than_sixty_four_characters"))
            return R.string.blog_name_must_be_less_than_sixty_four_characters;
        if (errorCode.equals("blog_name_contains_invalid_characters"))
            return R.string.blog_name_contains_invalid_characters;
        if (errorCode.equals("blog_name_cant_be_used"))
            return R.string.blog_name_cant_be_used;
        if (errorCode.equals("blog_name_only_lowercase_letters_and_numbers"))
            return R.string.blog_name_only_lowercase_letters_and_numbers;
        if (errorCode.equals("blog_name_must_include_letters"))
            return R.string.blog_name_must_include_letters;
        if (errorCode.equals("blog_name_exists"))
            return R.string.blog_name_exists;
        if (errorCode.equals("blog_name_reserved"))
            return R.string.blog_name_reserved;
        if (errorCode.equals("blog_name_reserved_but_may_be_available"))
            return R.string.blog_name_reserved_but_may_be_available;
        if (errorCode.equals("password_invalid"))
            return R.string.password_invalid;
        if (errorCode.equals("blog_name_invalid"))
            return R.string.blog_name_invalid;
        if (errorCode.equals("blog_title_invalid"))
            return R.string.blog_title_invalid;
        if (errorCode.equals("username_invalid"))
            return R.string.username_invalid;

        return 0;
    }
}