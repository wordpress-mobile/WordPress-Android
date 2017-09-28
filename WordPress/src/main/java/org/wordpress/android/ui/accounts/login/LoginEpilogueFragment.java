package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.login.util.ViewUtils;
import org.wordpress.android.ui.main.SitePickerAdapter;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;

import javax.inject.Inject;

public class LoginEpilogueFragment extends LoginBaseFormFragment<LoginEpilogueListener> {
    public static final String TAG = "login_epilogue_fragment_tag";

    private static final String ARG_DO_LOGIN_UPDATE = "ARG_DO_LOGIN_UPDATE";
    private static final String ARG_SHOW_AND_RETURN = "ARG_SHOW_AND_RETURN";
    private static final String ARG_OLD_SITES_IDS = "ARG_OLD_SITES_IDS";

    private RecyclerView mSitesList;
    private View mBottomShadow;
    private View mBottomButtonsContainer;
    private Button mConnectMore;

    @Inject AccountStore mAccountStore;

    private SitePickerAdapter mAdapter;
    private boolean mDoLoginUpdate;
    private boolean mShowAndReturn;
    private ArrayList<Integer> mOldSitesIds;

    private LoginEpilogueListener mLoginEpilogueListener;

    public static LoginEpilogueFragment newInstance(boolean doLoginUpdate, boolean showAndReturn,
            ArrayList<Integer> oldSitesIds) {
        LoginEpilogueFragment fragment = new LoginEpilogueFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_DO_LOGIN_UPDATE, doLoginUpdate);
        args.putBoolean(ARG_SHOW_AND_RETURN, showAndReturn);
        args.putIntegerArrayList(ARG_OLD_SITES_IDS, oldSitesIds);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean listenForLogin() {
        return mDoLoginUpdate;
    }

    @Override
    protected @LayoutRes int getContentLayout() {
        return 0; // nothing special here. The view is inflated in createMainView()
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return R.string.logging_in;
    }

    @Override
    protected void setupLabel(TextView label) {
        // nothing special to do, no main label on epilogue screen
    }

    @Override
    protected ViewGroup createMainView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return (ViewGroup) inflater.inflate(R.layout.login_epilogue_screen, container, false);
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        mBottomShadow = rootView.findViewById(R.id.bottom_shadow);

        mBottomButtonsContainer = rootView.findViewById(R.id.bottom_buttons);
        mConnectMore = (Button) mBottomButtonsContainer.findViewById(R.id.secondary_button);

        mSitesList = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mSitesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSitesList.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mSitesList.setItemAnimator(null);
        mSitesList.setAdapter(getAdapter());
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        secondaryButton.setVisibility(mShowAndReturn ? View.GONE : View.VISIBLE);
        secondaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginEpilogueListener != null) {
                    mLoginEpilogueListener.onConnectAnotherSite();
                }
            }
        });

        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginEpilogueListener != null) {
                    mLoginEpilogueListener.onContinue();
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        mDoLoginUpdate = getArguments().getBoolean(ARG_DO_LOGIN_UPDATE);
        mShowAndReturn = getArguments().getBoolean(ARG_SHOW_AND_RETURN);
        mOldSitesIds = getArguments().getIntegerArrayList(ARG_OLD_SITES_IDS);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_EPILOGUE_VIEWED);
        }
    }

    private SitePickerAdapter getAdapter() {
        if (mAdapter == null) {
            setNewAdapter();
        }
        return mAdapter;
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final View mLoggedInAsHeading;
        private final View mUserDetailsCard;
        private final WPNetworkImageView mAvatarImageView;
        private final TextView mDisplayNameTextView;
        private final TextView mUsernameTextView;
        private final TextView mMySitesHeadingTextView;

        HeaderViewHolder(View view) {
            super(view);
            mLoggedInAsHeading = view.findViewById(R.id.logged_in_as_heading);
            mUserDetailsCard = view.findViewById(R.id.user_details_card);
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
                mSitesList.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mSitesList.computeVerticalScrollRange() > mSitesList.getHeight()) {
                            mBottomShadow.setVisibility(View.VISIBLE);
                            mBottomButtonsContainer.setBackgroundResource(R.color.white);
                            ViewUtils.setButtonBackgroundColor(getContext(), mConnectMore,
                                    R.style.WordPress_Button_Grey, R.attr.colorButtonNormal);
                        } else {
                            mBottomShadow.setVisibility(View.GONE);
                            mBottomButtonsContainer.setBackground(null);
                            ViewUtils.setButtonBackgroundColor(getContext(), mConnectMore, R.style.WordPress_Button,
                                    R.attr.colorButtonNormal);
                        }
                    }
                });
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
        }, mOldSitesIds);
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
    public void onResume() {
        super.onResume();

        if (mDoLoginUpdate) {
            // when from magiclink, we need to complete the login process here (update account and settings)
            doFinishLogin();
        }
    }

    private void refreshAccountDetails(HeaderViewHolder holder, int numberOfSites) {
        if (!isAdded()) {
            return;
        }

        // we only want to show user details for WordPress.com users
        if (mAccountStore.hasAccessToken()) {
            AccountModel defaultAccount = mAccountStore.getAccount();

            holder.mLoggedInAsHeading.setVisibility(View.VISIBLE);
            holder.mUserDetailsCard.setVisibility(View.VISIBLE);

            final String avatarUrl = constructGravatarUrl(mAccountStore.getAccount());
            holder.mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR, null);

            holder.mUsernameTextView.setText(getString(R.string.login_username_at, defaultAccount.getUserName()));

            String displayName = defaultAccount.getDisplayName();
            if (!TextUtils.isEmpty(displayName)) {
                holder.mDisplayNameTextView.setText(displayName);
            } else {
                holder.mDisplayNameTextView.setText(defaultAccount.getUserName());
            }
        } else {
            holder.mLoggedInAsHeading.setVisibility(View.GONE);
            holder.mUserDetailsCard.setVisibility(View.GONE);
        }

        if (numberOfSites == 0) {
            holder.mMySitesHeadingTextView.setVisibility(View.GONE);

            mConnectMore.setText(R.string.connect_site);
        } else {
            holder.mMySitesHeadingTextView.setVisibility(View.VISIBLE);
            holder.mMySitesHeadingTextView.setText(
                    StringUtils.getQuantityString(
                            getActivity(), R.string.days_quantity_one, R.string.login_epilogue_mysites_one,
                            R.string.login_epilogue_mysites_other, numberOfSites));

            mConnectMore.setText(R.string.connect_more);
        }
    }

    private String constructGravatarUrl(AccountModel account) {
        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        return GravatarUtils.fixGravatarUrl(account.getAvatarUrl(), avatarSz);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    protected void onHelp() {
        // nothing to do. No help button on the epilogue screen
    }

    @Override
    protected void onLoginFinished() {
        // we needed to complete the login process so, now just show an updated screen to the user

        AnalyticsUtils.trackAnalyticsSignIn(mAccountStore, mSiteStore, true);

        endProgress();
        setNewAdapter();
        mSitesList.setAdapter(mAdapter);
    }
}
