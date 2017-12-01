package org.wordpress.android.ui.accounts.signup;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.TextView;

import org.wordpress.android.R;

public abstract class SiteCreationBaseFormFragment<SiteCreationListenerType> extends Fragment {

//    private static final String KEY_IN_PROGRESS = "KEY_IN_PROGRESS";
//    private static final String KEY_LOGIN_FINISHED = "KEY_LOGIN_FINISHED";

    private Button mPrimaryButton;
    private Button mSecondaryButton;
//    private ProgressDialog mProgressDialog;

    protected SiteCreationListenerType mSiteCreationListener;

//    private boolean mInProgress;
//    private boolean mLoginFinished;

//    @Inject Dispatcher mDispatcher;
//    @Inject SiteStore mSiteStore;
//    @Inject AccountStore mAccountStore;

    protected abstract @LayoutRes int getContentLayout();
    protected abstract void setupContent(ViewGroup rootView);
    protected abstract boolean setupBottomButtons(Button secondaryButton, Button primaryButton);
//    protected abstract @StringRes int getProgressBarText();

//    protected boolean listenForLogin() {
//        return true;
//    }

//    protected EditText getEditTextToFocusOnStart() {
//        return null;
//    }

//    protected boolean isInProgress() {
//        return mInProgress;
//    }

//    protected Button getPrimaryButton() {
//        return mPrimaryButton;
//    }

    protected abstract void onHelp();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    protected ViewGroup createMainView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.site_creation_form_screen, container, false);
        ViewStub form_container = ((ViewStub) rootView.findViewById(R.id.site_creation_form_content_stub));
        form_container.setLayoutResource(getContentLayout());
        form_container.inflate();
        return rootView;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = createMainView(inflater, container, savedInstanceState);

        setupContent(rootView);

        mPrimaryButton = (Button) rootView.findViewById(R.id.primary_button);
        mSecondaryButton = (Button) rootView.findViewById(R.id.secondary_button);
        if (setupBottomButtons(mSecondaryButton, mPrimaryButton)) {
            rootView.findViewById(R.id.bottom_buttons).setVisibility(View.GONE);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.site_creation_title);
        }

//        if (savedInstanceState == null) {
//            EditTextUtils.showSoftInput(getEditTextToFocusOnStart());
//        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

//        if (savedInstanceState != null) {
//            mInProgress = savedInstanceState.getBoolean(KEY_IN_PROGRESS);
//            mLoginFinished = savedInstanceState.getBoolean(KEY_LOGIN_FINISHED);
//
//            if (mInProgress) {
//                startProgress();
//            }
//        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onAttach(Context context) {
        super.onAttach(context);

        // this will throw if parent activity doesn't implement the login listener interface
        mSiteCreationListener = (SiteCreationListenerType) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSiteCreationListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();

//        if (listenForLogin()) {
//            mDispatcher.register(this);
//        }
    }

    @Override
    public void onStop() {
        super.onStop();

//        if (listenForLogin()) {
//            mDispatcher.unregister(this);
//        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

//        outState.putBoolean(KEY_IN_PROGRESS, mInProgress);
//        outState.putBoolean(KEY_LOGIN_FINISHED, mLoginFinished);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_login, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.help) {
            onHelp();
            return true;
        }

        return false;
    }

//    protected void startProgress() {
//        startProgress(true);
//    }

//    protected void startProgress(boolean cancellable) {
//        mPrimaryButton.setEnabled(false);
//        mSecondaryButton.setEnabled(false);
//
//        mProgressDialog =
//                ProgressDialog.show(getActivity(), "", getActivity().getString(getProgressBarText()), true, cancellable,
//                        new DialogInterface.OnCancelListener() {
//                            @Override
//                            public void onCancel(DialogInterface dialogInterface) {
//                                if (isInProgress()) {
//                                    endProgress();
//                                }
//                            }
//                        });
//        mInProgress = true;
//    }

//    @CallSuper
//    protected void endProgress() {
//        mInProgress = false;
//
//        if (mProgressDialog != null) {
//            mProgressDialog.cancel();
//            mProgressDialog = null;
//        }
//
//        mPrimaryButton.setEnabled(true);
//        mSecondaryButton.setEnabled(true);
//    }

//    protected void doFinishLogin() {
//        if (mLoginFinished) {
//            onLoginFinished(false);
//            return;
//        }
//
//        if (mProgressDialog == null) {
//            startProgress();
//        }
//
//        mProgressDialog.setCancelable(false);
//        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
//    }

//    protected void onLoginFinished() {
//    }

//    protected void onLoginFinished(boolean success) {
//        mLoginFinished = true;
//
//        if (!success) {
//            endProgress();
//            return;
//        }
//
//        if (mLoginListener != null) {
//            onLoginFinished();
//        }
//    }

//    protected void startPostLoginServices() {
//        // Get reader tags so they're available as soon as the Reader is accessed - done for
//        // both wp.com and self-hosted (self-hosted = "logged out" reader) - note that this
//        // uses the application context since the activity is finished immediately below
//        ReaderUpdateService.startService(getActivity().getApplicationContext(), EnumSet.of(ReaderUpdateService
//                .UpdateTask.TAGS));
//
//        // Start Notification service
//        NotificationsUpdateService.startService(getActivity().getApplicationContext());
//    }

//    /*
//     * auto-fill the username and password from BuildConfig/gradle.properties (developer feature,
//     * only enabled for DEBUG releases)
//     */
//    protected void autoFillFromBuildConfig(String configValueName, TextView textView) {
//        if (!BuildConfig.DEBUG) return;
//
//        String value = (String) WordPress.getBuildConfigValue(getActivity().getApplication(), configValueName);
//        if (!TextUtils.isEmpty(value)) {
//            textView.setText(value);
//            AppLog.d(AppLog.T.NUX, "Auto-filled from build config: " + configValueName);
//        }
//    }

    // OnChanged events

//    @SuppressWarnings("unused")
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onAccountChanged(AccountStore.OnAccountChanged event) {
//        if (!isAdded() || mLoginFinished) {
//            return;
//        }
//
//        if (event.isError()) {
//            AppLog.e(AppLog.T.API, "onAccountChanged has error: " + event.error.type + " - " + event.error.message);
//            ToastUtils.showToast(getContext(), R.string.error_fetch_my_profile);
//            onLoginFinished(false);
//            return;
//        }
//
//        if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
//            // The user's account info has been fetched and stored - next, fetch the user's settings
//            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
//        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
//            // The user's account settings have also been fetched and stored - now we can fetch the user's sites
//            mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
//        }
//    }
//
//    @SuppressWarnings("unused")
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onSiteChanged(SiteStore.OnSiteChanged event) {
//        if (!isAdded() || mLoginFinished) {
//            return;
//        }
//
//        if (event.isError()) {
//            AppLog.e(AppLog.T.API, "onSiteChanged has error: " + event.error.type + " - " + event.error.toString());
//            if (!isAdded() || event.error.type != SiteStore.SiteErrorType.DUPLICATE_SITE) {
//                onLoginFinished(false);
//                return;
//            }
//
//            if (event.rowsAffected == 0) {
//                // If there is a duplicate site and not any site has been added, show an error and
//                // stop the sign in process
//                ToastUtils.showToast(getContext(), R.string.cannot_add_duplicate_site);
//                onLoginFinished(false);
//                return;
//            } else {
//                // If there is a duplicate site, notify the user something could be wrong,
//                // but continue the sign in process
//                ToastUtils.showToast(getContext(), R.string.duplicate_site_detected);
//            }
//        }
//
//        startPostLoginServices();
//
//        onLoginFinished(true);
//    }
}
