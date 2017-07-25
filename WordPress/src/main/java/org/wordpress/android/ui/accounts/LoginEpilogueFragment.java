package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.ui.main.SitePickerAdapter;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;

import javax.inject.Inject;

public class LoginEpilogueFragment extends android.support.v4.app.Fragment {
    public static final String TAG = "login_epilogue_fragment_tag";

    private static final String ARG_SHOW_AND_RETURN = "ARG_SHOW_AND_RETURN";
    private static final String ARG_OLD_SITES_IDS = "ARG_OLD_SITES_IDS";

    private View mSitesProgress;
    private RecyclerView mSitesList;
    private View mBottomShadow;
    private View mBottomButtonsContainer;

    protected @Inject AccountStore mAccountStore;

    private SitePickerAdapter mAdapter;
    private boolean mShowAndReturn;
    private ArrayList<Integer> mOldSitesIds;

    interface LoginEpilogueListener {
        void onConnectAnotherSite();
        void onContinue();
    }
    private LoginEpilogueListener mLoginEpilogueListener;

    public static LoginEpilogueFragment newInstance(boolean showAndReturn, ArrayList<Integer> oldSitesIds) {
        LoginEpilogueFragment fragment = new LoginEpilogueFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SHOW_AND_RETURN, showAndReturn);
        args.putIntegerArrayList(ARG_OLD_SITES_IDS, oldSitesIds);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        mShowAndReturn = getArguments().getBoolean(ARG_SHOW_AND_RETURN);
        mOldSitesIds = getArguments().getIntegerArrayList(ARG_OLD_SITES_IDS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_epilogue_screen, container, false);

        mSitesProgress = rootView.findViewById(R.id.sites_progress);

        mBottomShadow = rootView.findViewById(R.id.bottom_shadow);
        mBottomButtonsContainer = rootView.findViewById(R.id.bottom_buttons);

        View connectMore = rootView.findViewById(R.id.login_connect_more);
        connectMore.setVisibility(mShowAndReturn ? View.GONE : View.VISIBLE);
        connectMore.setOnClickListener(new View.OnClickListener() {
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
                if (mSitesList.computeVerticalScrollRange() > mSitesList.getHeight()) {
                    mBottomShadow.setVisibility(View.VISIBLE);
                    mBottomButtonsContainer.setBackgroundResource(R.color.white);
                } else {
                    mBottomShadow.setVisibility(View.GONE);
                    mBottomButtonsContainer.setBackground(null);
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
        }, mOldSitesIds);
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

            holder.mLoggedInAsHeading.setVisibility(View.VISIBLE);
            holder.mUserDetailsCard.setVisibility(View.VISIBLE);

            final String avatarUrl = constructGravatarUrl(mAccountStore.getAccount());
            holder.mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR, null);

            holder.mUsernameTextView.setText(getString(R.string.login_username_at, defaultAccount.getUserName()));

            String displayName = StringUtils.unescapeHTML(defaultAccount.getDisplayName());
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
        } else {
            holder.mMySitesHeadingTextView.setVisibility(View.VISIBLE);
            holder.mMySitesHeadingTextView.setText(
                    StringUtils.getQuantityString(
                            getActivity(), R.string.days_quantity_one, R.string.login_epilogue_mysites_one,
                            R.string.login_epilogue_mysites_other, numberOfSites));
        }
    }

    private String constructGravatarUrl(AccountModel account) {
        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        return GravatarUtils.fixGravatarUrl(account.getAvatarUrl(), avatarSz);
    }
}
