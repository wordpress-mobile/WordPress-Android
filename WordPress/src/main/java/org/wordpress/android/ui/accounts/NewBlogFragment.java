package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.accounts.helpers.CreateUserAndBlog;
import org.wordpress.android.ui.plans.PlansConstants;
import org.wordpress.android.util.AlertUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPTextView;

public class NewBlogFragment extends AbstractFragment implements TextWatcher {
    private EditText mSiteUrlTextField;
    private EditText mSiteTitleTextField;
    private WPTextView mSignupButton;
    private WPTextView mProgressTextSignIn;
    private WPTextView mCancelButton;
    private RelativeLayout mProgressBarSignIn;
    private boolean mSignoutOnCancelMode;
    private boolean mAutoCompleteUrl;

    public NewBlogFragment() {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        checkIfFieldsFilled();
    }

    public void setSignoutOnCancelMode(boolean mode) {
        mSignoutOnCancelMode = mode;
        mCancelButton.setVisibility(View.VISIBLE);
    }

    public boolean isSignoutOnCancelMode() {
        return mSignoutOnCancelMode;
    }

    public void onBackPressed() {
        signoutAndFinish();
    }

    protected void onDoneAction() {
        validateAndCreateUserAndBlog();
    }

    private void signoutAndFinish() {
        if (mSignoutOnCancelMode) {
            WordPress.WordPressComSignOut(getActivity());
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
        }
    }

    private boolean fieldsFilled() {
        return EditTextUtils.getText(mSiteUrlTextField).trim().length() > 0
                && EditTextUtils.getText(mSiteTitleTextField).trim().length() > 0;
    }

    protected void startProgress(String message) {
        mProgressBarSignIn.setVisibility(View.VISIBLE);
        mProgressTextSignIn.setVisibility(View.VISIBLE);
        mSignupButton.setVisibility(View.GONE);
        mProgressBarSignIn.setEnabled(false);
        mProgressTextSignIn.setText(message);
        mSiteTitleTextField.setEnabled(false);
        mSiteUrlTextField.setEnabled(false);
    }

    protected void updateProgress(String message) {
        mProgressTextSignIn.setText(message);
    }

    protected void endProgress() {
        mProgressBarSignIn.setVisibility(View.GONE);
        mProgressTextSignIn.setVisibility(View.GONE);
        mSignupButton.setVisibility(View.VISIBLE);
        mSiteTitleTextField.setEnabled(true);
        mSiteUrlTextField.setEnabled(true);
    }

    private void showSiteUrlError(int messageId) {
        mSiteUrlTextField.setError(getString(messageId));
        mSiteUrlTextField.requestFocus();
    }

    private void showSiteTitleError(int messageId) {
        mSiteTitleTextField.setError(getString(messageId));
        mSiteTitleTextField.requestFocus();
    }

    protected boolean specificShowError(int messageId) {
        switch (getErrorType(messageId)) {
            case TITLE:
                showSiteTitleError(messageId);
                return true;
            case SITE_URL:
                showSiteUrlError(messageId);
                return true;
        }
        return false;
    }

    protected boolean isUserDataValid() {
        final String siteTitle = EditTextUtils.getText(mSiteTitleTextField).trim();
        final String siteUrl = EditTextUtils.getText(mSiteUrlTextField).trim();
        boolean retValue = true;

        if (siteTitle.equals("")) {
            mSiteTitleTextField.setError(getString(R.string.required_field));
            mSiteTitleTextField.requestFocus();
            retValue = false;
        }

        if (siteUrl.equals("")) {
            mSiteUrlTextField.setError(getString(R.string.required_field));
            mSiteUrlTextField.requestFocus();
            retValue = false;
        }
        return retValue;
    }

    private String titleToUrl(String siteUrl) {
        return siteUrl.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private void validateAndCreateUserAndBlog() {
        if (mSystemService.getActiveNetworkInfo() == null) {
            AlertUtils.showAlert(getActivity(), R.string.no_network_title, R.string.no_network_message);
            return;
        }
        if (!isUserDataValid()) {
            return;
        }

        // prevent double tapping of the "done" btn in keyboard for those clients that don't dismiss the keyboard.
        // Samsung S4 for example
        if (View.VISIBLE == mProgressBarSignIn.getVisibility()) {
            return;
        }

        startProgress(getString(R.string.validating_site_data));

        final String siteUrl = EditTextUtils.getText(mSiteUrlTextField).trim();
        final String siteName = EditTextUtils.getText(mSiteTitleTextField).trim();
        final String language = CreateUserAndBlog.getDeviceLanguage(getActivity());

        CreateUserAndBlog createUserAndBlog = new CreateUserAndBlog("", "", "", siteUrl, siteName, language,
                getRestClientUtils(), getActivity(), new ErrorListener(), new CreateUserAndBlog.Callback() {
            @Override
            public void onStepFinished(CreateUserAndBlog.Step step) {
                if (getActivity() != null) {
                    updateProgress(getString(R.string.create_new_blog_wpcom));
                }
            }

            @Override
            public void onSuccess(JSONObject createSiteResponse) {
                if (getActivity() == null) {
                    return;
                }
                endProgress();
                try {
                    JSONObject details = createSiteResponse.getJSONObject("blog_details");
                    String blogName = details.getString("blogname");
                    String xmlRpcUrl = details.getString("xmlrpc");
                    String homeUrl = details.getString("url");
                    String blogId = details.getString("blogid");
                    String username = AccountHelper.getDefaultAccount().getUserName();
                    BlogUtils.addOrUpdateBlog(blogName, xmlRpcUrl, homeUrl, blogId, username, null, null, null,
                            true, true, PlansConstants.DEFAULT_PLAN_ID_FOR_NEW_BLOG, null, null);
                    ToastUtils.showToast(getActivity(), R.string.new_blog_wpcom_created);
                } catch (JSONException e) {
                    AppLog.e(T.NUX, "Invalid JSON response from site/new", e);
                }
                getActivity().setResult(Activity.RESULT_OK);
                getActivity().finish();
            }

            @Override
            public void onError(int messageId) {
                if (getActivity() == null) {
                    return;
                }
                endProgress();
                showError(getString(messageId));
            }
        });
        AppLog.i(T.NUX, "User tries to create a new site, name: " + siteName + ", URL: " + siteUrl);
        createUserAndBlog.startCreateBlogProcess();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.create_blog_fragment, container, false);

        mSignupButton = (WPTextView) rootView.findViewById(R.id.signup_button);
        mSignupButton.setOnClickListener(mSignupClickListener);
        mSignupButton.setEnabled(false);

        mCancelButton = (WPTextView) rootView.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(mCancelClickListener);

        mProgressTextSignIn = (WPTextView) rootView.findViewById(R.id.nux_sign_in_progress_text);
        mProgressBarSignIn = (RelativeLayout) rootView.findViewById(R.id.nux_sign_in_progress_bar);

        mSiteUrlTextField = (EditText) rootView.findViewById(R.id.site_url);
        mSiteUrlTextField.setOnKeyListener(mSiteUrlKeyListener);
        mSiteUrlTextField.setOnEditorActionListener(mEditorAction);
        mSiteUrlTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkIfFieldsFilled();
            }

            @Override
            public void afterTextChanged(Editable s) {
                BlogUtils.convertToLowercase(s);
            }
        });

        mSiteTitleTextField = (EditText) rootView.findViewById(R.id.site_title);
        mSiteTitleTextField.addTextChangedListener(this);
        mSiteTitleTextField.addTextChangedListener(mSiteTitleWatcher);
        mSiteTitleTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mAutoCompleteUrl = EditTextUtils.getText(mSiteTitleTextField)
                            .equals(EditTextUtils.getText(mSiteUrlTextField))
                            || EditTextUtils.isEmpty(mSiteUrlTextField);
                }
            }
        });
        return rootView;
    }

    private void checkIfFieldsFilled() {
        if (fieldsFilled()) {
            mSignupButton.setEnabled(true);
        } else {
            mSignupButton.setEnabled(false);
        }
    }

    private final OnClickListener mSignupClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            validateAndCreateUserAndBlog();
        }
    };

    private final OnClickListener mCancelClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            signoutAndFinish();
        }
    };

    private final TextWatcher mSiteTitleWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // auto fill blog address from title if user hasn't modified url
            if (mAutoCompleteUrl) {
                mSiteUrlTextField.setText(titleToUrl(EditTextUtils.getText(mSiteTitleTextField)));
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private final OnKeyListener mSiteUrlKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            mAutoCompleteUrl = EditTextUtils.isEmpty(mSiteUrlTextField);
            return false;
        }
    };

    private final TextView.OnEditorActionListener mEditorAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            return onDoneEvent(actionId, event);
        }
    };
}
