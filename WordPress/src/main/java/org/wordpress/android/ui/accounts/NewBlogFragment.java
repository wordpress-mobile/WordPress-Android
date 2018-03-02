package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType;
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload;
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains;
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.util.AlertUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.LanguageUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.widgets.WPTextView;

import javax.inject.Inject;

public class NewBlogFragment extends AbstractFragment implements TextWatcher {
    private AutoCompleteTextView mSiteUrlTextField;
    private EditText mSiteTitleTextField;
    private ArrayAdapter<String> mSiteUrlSuggestionAdapter;
    private WPTextView mSignupButton;
    private WPTextView mProgressTextSignIn;
    private WPTextView mCancelButton;
    private RelativeLayout mProgressBarSignIn;

    private boolean mSignoutOnCancelMode;

    private long mNewSiteRemoteId;

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
        mSiteUrlSuggestionAdapter = new ArrayAdapter<>(getActivity(), R.layout.domain_suggestion_dropdown);
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
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
            ((WordPress) getActivity().getApplication()).wordPressComSignOut();
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

    private void showSiteUrlError(String message) {
        mSiteUrlTextField.setError(message);
        mSiteUrlTextField.requestFocus();
    }

    private void showSiteTitleError(String message) {
        mSiteTitleTextField.setError(message);
        mSiteTitleTextField.requestFocus();
    }

    protected boolean showError(NewSiteErrorType newSiteError, String message) {
        if (!isAdded()) {
            return false;
        }
        switch (newSiteError) {
            case SITE_TITLE_INVALID:
                showSiteTitleError(message);
                return true;
            default:
                showSiteUrlError(message);
                return true;
        }
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

        startProgress(getString(R.string.creating_your_site));

        final String siteUrl = EditTextUtils.getText(mSiteUrlTextField).trim();
        final String siteTitle = EditTextUtils.getText(mSiteTitleTextField).trim();
        final String language = LanguageUtils.getPatchedCurrentDeviceLanguage(getActivity());

        NewSitePayload newSitePayload = new NewSitePayload(siteUrl, siteTitle, language, SiteVisibility.PUBLIC, false);
        mDispatcher.dispatch(SiteActionBuilder.newCreateNewSiteAction(newSitePayload));
        AppLog.i(T.NUX, "User tries to create a new site, title: " + siteTitle + ", URL: " + siteUrl);
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

        mSiteUrlTextField = (AutoCompleteTextView) rootView.findViewById(R.id.site_url);
        mSiteUrlTextField.setAdapter(mSiteUrlSuggestionAdapter);
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
            public void afterTextChanged(Editable editable) {
                lowerCaseEditable(editable);
            }
        });
        mSiteUrlTextField.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !mSiteUrlSuggestionAdapter.isEmpty()) {
                    mSiteUrlTextField.showDropDown();
                }
            }
        });

        mSiteTitleTextField = (EditText) rootView.findViewById(R.id.site_title);
        mSiteTitleTextField.addTextChangedListener(this);
        mSiteTitleTextField.addTextChangedListener(mSiteTitleWatcher);
        mSiteTitleTextField.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    getDomainSuggestionsFromTitle();
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
        }

        @Override
        public void afterTextChanged(Editable s) {
            mSiteUrlSuggestionAdapter.clear();
            mSiteUrlSuggestionAdapter.notifyDataSetChanged();
        }
    };

    private void getDomainSuggestionsFromTitle() {
        String title = EditTextUtils.getText(mSiteTitleTextField);
        if (!TextUtils.isEmpty(title)) {
            SuggestDomainsPayload payload = new SuggestDomainsPayload(title, false, true, false, 5);
            mDispatcher.dispatch(SiteActionBuilder.newSuggestDomainsAction(payload));
        }
    }

    private final TextView.OnEditorActionListener mEditorAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            return onDoneEvent(actionId, event);
        }
    };

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewSiteCreated(OnNewSiteCreated event) {
        AppLog.i(T.NUX, event.toString());
        if (event.isError()) {
            endProgress();
            showError(event.error.type, event.error.message);
            return;
        }
        AnalyticsTracker.track(AnalyticsTracker.Stat.CREATED_SITE);
        mNewSiteRemoteId = event.newSiteRemoteId;
        // We can't get all the site informations from the new site endpoint, so we have to fetch the site list.
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.NUX, event.toString());
        // Sites updated, we can finish this.
        if (getActivity() == null) {
            return;
        }
        endProgress();
        if (event.isError()) {
            // Site has been created but there was a error while fetching the sites. Can happen if we get
            // a response including a broken Jetpack site. We can continue and check if the newly created
            // site has been fetched.
            AppLog.e(T.NUX, event.error.type.toString());
        }
        SiteModel site = mSiteStore.getSiteBySiteId(mNewSiteRemoteId);
        Intent intent = new Intent();
        if (site != null) {
            intent.putExtra(SitePickerActivity.KEY_LOCAL_ID, site.getId());
        } else {
            ToastUtils.showToast(getActivity(), R.string.error_fetch_site_after_creation, Duration.LONG);
        }
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSuggestedDomains(OnSuggestedDomains event) {
        if (!isAdded() || event.isError()) {
            return;
        }

        mSiteUrlSuggestionAdapter.clear();
        for (DomainSuggestionResponse suggestion : event.suggestions) {
            // Only add free suggestions ending by .wordpress.com
            if (suggestion.is_free && !TextUtils.isEmpty(suggestion.domain_name)
                    && suggestion.domain_name.endsWith(".wordpress.com")) {
                mSiteUrlSuggestionAdapter.add(suggestion.domain_name.replace(".wordpress.com", ""));
            }
        }
        if (!mSiteUrlSuggestionAdapter.isEmpty() && mSiteUrlTextField.hasFocus()) {
            mSiteUrlTextField.showDropDown();
        }
    }
}
