package org.wordpress.android.ui.accounts;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.main.SitePickerAdapter;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

public class LoginEpilogueFragment extends android.support.v4.app.Fragment {
    public static final String TAG = "login_epilogue_fragment_tag";

    private static final String KEY_IN_PROGRESS = "KEY_IN_PROGRESS";

    private WPNetworkImageView mAvatarImageView;
    private TextView mDisplayNameTextView;
    private TextView mUsernameTextView;
    private View mSitesProgress;

    protected @Inject SiteStore mSiteStore;
    protected @Inject AccountStore mAccountStore;
    protected @Inject Dispatcher mDispatcher;

    private boolean mInProgress;
    private SitePickerAdapter mAdapter;

    public interface LoginEpilogueListener {
        void onConnectAnotherSite();
        void onContinue();
    }
    private LoginEpilogueListener mLoginEpilogueListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_epilogue_screen, container, false);

        mAvatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.avatar);
        mDisplayNameTextView = (TextView) rootView.findViewById(R.id.display_name);
        mUsernameTextView = (TextView) rootView.findViewById(R.id.username);

        mSitesProgress = rootView.findViewById(R.id.sites_progress);

        rootView.findViewById(R.id.login_connect_more).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginEpilogueListener != null) {
                    mLoginEpilogueListener.onConnectAnotherSite();
                }
            }
        });

        rootView.findViewById(R.id.login_continue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginEpilogueListener != null) {
                    mLoginEpilogueListener.onContinue();
                }
            }
        });

        RecyclerView sitesList = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        sitesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        sitesList.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        sitesList.setItemAnimator(null);
        sitesList.setAdapter(getAdapter());

        refreshAccountDetails();

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            mInProgress = true;
            mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
            mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
        } else {
            mInProgress = savedInstanceState.getBoolean(KEY_IN_PROGRESS);
        }

        showProgress(mInProgress);
    }

    private SitePickerAdapter getAdapter() {
        if (mAdapter == null) {
            setNewAdapter();
        }
        return mAdapter;
    }

    private void setNewAdapter() {
        mAdapter = new SitePickerAdapter(
                getActivity(),
                0,
                "",
                false,
                null);
    }

    public void showProgress(boolean show) {
        if (mSitesProgress != null) {
            mSitesProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoginEpilogueListener) {
            mLoginEpilogueListener = (LoginEpilogueListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement LoginEpilogueListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_IN_PROGRESS, mInProgress);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void refreshAccountDetails() {
        // we only want to show user details for WordPress.com users
        if (mAccountStore.hasAccessToken()) {
            AccountModel defaultAccount = mAccountStore.getAccount();

            mDisplayNameTextView.setVisibility(View.VISIBLE);
            mUsernameTextView.setVisibility(View.VISIBLE);

            final String avatarUrl = constructGravatarUrl(mAccountStore.getAccount());
            mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR, null);

            mUsernameTextView.setText(getString(R.string.login_username_at, defaultAccount.getUserName()));

            String displayName = StringUtils.unescapeHTML(defaultAccount.getDisplayName());
            if (!TextUtils.isEmpty(displayName)) {
                mDisplayNameTextView.setText(displayName);
            } else {
                mDisplayNameTextView.setText(defaultAccount.getUserName());
            }
        } else {
            mDisplayNameTextView.setVisibility(View.GONE);
            mUsernameTextView.setVisibility(View.GONE);
        }
    }

    private String constructGravatarUrl(AccountModel account) {
        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        return GravatarUtils.fixGravatarUrl(account.getAvatarUrl(), avatarSz);
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(AccountStore.OnAccountChanged event) {
        if (!isAdded() || event.causeOfChange != AccountAction.FETCH_ACCOUNT) {
            return;
        }

        if (event.isError()) {
            AppLog.e(AppLog.T.API, "onAccountChanged has error: " + event.error.type + " - " + event.error.message);
            ToastUtils.showToast(getContext(), R.string.error_fetch_my_profile);
        }

        refreshAccountDetails();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "onSiteChanged has error: " + event.error.type + " - " + event.error.toString());
            if (!isAdded() || event.error.type != SiteStore.SiteErrorType.DUPLICATE_SITE) {
                return;
            }

            if (event.rowsAffected == 0) {
                // If there is a duplicate site and not any site has been added, show an error and
                // stop the sign in process
                ToastUtils.showToast(getContext(), R.string.cannot_add_duplicate_site);
                return;
            } else {
                // If there is a duplicate site, notify the user something could be wrong,
                // but continue the sign in process
                ToastUtils.showToast(getContext(), R.string.duplicate_site_detected);
            }
        }

        // Start Notification service
        NotificationsUpdateService.startService(getActivity().getApplicationContext());

        mInProgress = false;
        showProgress(false);
        getAdapter().loadSites();
    }
}
