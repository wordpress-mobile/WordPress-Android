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

    private static final String ARG_SHOW_AND_RETURN = "ARG_SHOW_AND_RETURN";

    private View mSitesProgress;
    private RecyclerView mSitesList;
    private View mBottomShadow;
    private View mBottomButtonsContainer;
    private View mConnectMore;
    private View mConnectMoreGrey;

    protected @Inject SiteStore mSiteStore;
    protected @Inject AccountStore mAccountStore;
    protected @Inject Dispatcher mDispatcher;

    private boolean mInProgress;
    private SitePickerAdapter mAdapter;
    private boolean mShowAndReturn;

    interface LoginEpilogueListener {
        void onConnectAnotherSite();
        void onContinue();
    }
    private LoginEpilogueListener mLoginEpilogueListener;

    public static LoginEpilogueFragment newInstance(boolean showAndReturn) {
        LoginEpilogueFragment fragment = new LoginEpilogueFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SHOW_AND_RETURN, showAndReturn);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        mShowAndReturn = getArguments().getBoolean(ARG_SHOW_AND_RETURN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_epilogue_screen, container, false);

        mSitesProgress = rootView.findViewById(R.id.sites_progress);

        mBottomShadow = rootView.findViewById(R.id.bottom_shadow);
        mBottomButtonsContainer = rootView.findViewById(R.id.bottom_buttons);

        mConnectMore = rootView.findViewById(R.id.login_connect_more);
        mConnectMore.setVisibility(mShowAndReturn ? View.GONE : View.VISIBLE);
        mConnectMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginEpilogueListener != null) {
                    mLoginEpilogueListener.onConnectAnotherSite();
                }
            }
        });

        mConnectMoreGrey = rootView.findViewById(R.id.login_connect_more_grey);
        mConnectMoreGrey.setVisibility(View.GONE); // hide the grey version of the button on start
        mConnectMoreGrey.setOnClickListener(new View.OnClickListener() {
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

        mSitesList = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mSitesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSitesList.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mSitesList.setItemAnimator(null);
        mSitesList.setAdapter(getAdapter());

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            if (mAccountStore.hasAccessToken()) {
                mInProgress = true;
                mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
            }
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

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final WPNetworkImageView mAvatarImageView;
        private final TextView mDisplayNameTextView;
        private final TextView mUsernameTextView;
        private final TextView mMySitesHeadingTextView;

        HeaderViewHolder(View view) {
            super(view);
            mAvatarImageView = (WPNetworkImageView) view.findViewById(R.id.avatar);
            mDisplayNameTextView = (TextView) view.findViewById(R.id.display_name);
            mUsernameTextView = (TextView) view.findViewById(R.id.username);
            mMySitesHeadingTextView = (TextView) view.findViewById(R.id.my_sites_heading);
        }
    }

    private void setNewAdapter() {
        mAdapter = new SitePickerAdapter(getActivity(), R.layout.login_epilogue_sites_listitem, 0, "", false,
                new SitePickerAdapter.OnDataLoadedListener() {
            @Override
            public void onBeforeLoad(boolean isEmpty) {}

            @Override
            public void onAfterLoad() {
                if (mSitesList.computeVerticalScrollRange() > mSitesList.getHeight()) {
                    mBottomShadow.setVisibility(View.VISIBLE);
                    mBottomButtonsContainer.setBackgroundResource(R.color.white);
                    mConnectMore.setVisibility(View.GONE);
                    mConnectMoreGrey.setVisibility(View.VISIBLE);
                } else {
                    mBottomShadow.setVisibility(View.GONE);
                    mBottomButtonsContainer.setBackground(null);
                    mConnectMore.setVisibility(View.VISIBLE);
                    mConnectMoreGrey.setVisibility(View.GONE);
                }
            }
        }, new SitePickerAdapter.HeaderHandler() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater layoutInflater, ViewGroup parent, boolean attachToRoot) {
                return new HeaderViewHolder(layoutInflater.inflate(R.layout.login_epilogue_header, parent, false));
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int numberOfSites) {
                refreshAccountDetails((HeaderViewHolder) holder, numberOfSites);
            }
        });
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

    private void refreshAccountDetails(HeaderViewHolder holder, int numberOfSites) {
        if (!isAdded()) {
            return;
        }

        // we only want to show user details for WordPress.com users
        if (mAccountStore.hasAccessToken()) {
            AccountModel defaultAccount = mAccountStore.getAccount();

            holder.mDisplayNameTextView.setVisibility(View.VISIBLE);
            holder.mUsernameTextView.setVisibility(View.VISIBLE);

            final String avatarUrl = constructGravatarUrl(mAccountStore.getAccount());
            holder.mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR, null);

            holder.mUsernameTextView.setText(getString(R.string.login_username_at, defaultAccount.getUserName()));

            String displayName = StringUtils.unescapeHTML(defaultAccount.getDisplayName());
            if (!TextUtils.isEmpty(displayName)) {
                holder.mDisplayNameTextView.setText(displayName);
            } else {
                holder.mDisplayNameTextView.setText(defaultAccount.getUserName());
            }

            if (numberOfSites == 0) {
                holder.mMySitesHeadingTextView.setVisibility(View.GONE);
            } else {
                holder.mMySitesHeadingTextView.setVisibility(View.VISIBLE);
                holder.mMySitesHeadingTextView.setText(
                        StringUtils.getQuantityString(
                                getActivity(), R.string.days_quantity_one, R.string.login_epilogue_mysites_one,
                                        R.string.login_epilogue_mysites_other, numberOfSites));
            }

        } else {
            holder.mDisplayNameTextView.setVisibility(View.GONE);
            holder.mUsernameTextView.setVisibility(View.GONE);
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
        if (!isAdded()) {
            return;
        }

        if (event.isError()) {
            AppLog.e(AppLog.T.API, "onAccountChanged has error: " + event.error.type + " - " + event.error.message);
            ToastUtils.showToast(getContext(), R.string.error_fetch_my_profile);
            return;
        }

        if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
            getAdapter().notifyDataSetChanged();

            // The user's account info has been fetched and stored - next, fetch the user's settings
            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            // The user's account settings have also been fetched and stored - now we can fetch the user's sites
            mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
        }
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
