package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.login.LoginBaseFormFragment;
import org.wordpress.android.ui.main.SitePickerAdapter;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteList;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.image.ImageManager;

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

    private SitePickerAdapter mAdapter;
    private boolean mDoLoginUpdate;
    private boolean mShowAndReturn;
    private ArrayList<Integer> mOldSitesIds;

    private LoginEpilogueListener mLoginEpilogueListener;

    @Inject ImageManager mImageManager;

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
    protected void setupLabel(@NonNull TextView label) {
        // nothing special to do, no main label on epilogue screen
    }

    @Override
    protected ViewGroup createMainView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return (ViewGroup) inflater.inflate(R.layout.login_epilogue_screen, container, false);
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        mBottomShadow = rootView.findViewById(R.id.bottom_shadow);

//        mBottomButtonsContainer = rootView.findViewById(R.id.bottom_buttons);
//        mConnectMore = mBottomButtonsContainer.findViewById(R.id.secondary_button);

        mSitesList = rootView.findViewById(R.id.recycler_view);
        mSitesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSitesList.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mSitesList.setItemAnimator(null);
        mSitesList.setAdapter(getAdapter());
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
//        secondaryButton.setVisibility(mShowAndReturn ? View.GONE : View.VISIBLE);
//        secondaryButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mLoginEpilogueListener != null) {
//                    mLoginEpilogueListener.onConnectAnotherSite();
//                }
//            }
//        });

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

    private void setNewAdapter() {
        mAdapter = new SitePickerAdapter(
                getActivity(), R.layout.login_epilogue_sites_listitem, 0, "", false,
                new SitePickerAdapter.OnDataLoadedListener() {
                    @Override
                    public void onBeforeLoad(boolean isEmpty) {
                    }

                    @Override
                    public void onAfterLoad() {
                        mSitesList.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!isAdded()) {
                                    return;
                                }

                                if (mSitesList.computeVerticalScrollRange() > mSitesList.getHeight()) {
                                    mBottomShadow.setVisibility(View.VISIBLE);
                                } else {
                                    mBottomShadow.setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                }, new SitePickerAdapter.HeaderHandler() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater layoutInflater, ViewGroup parent,
                                                              boolean attachToRoot) {
                return new LoginHeaderViewHolder(layoutInflater.inflate(R.layout.login_epilogue_header, parent, false));
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, SiteList sites) {
                refreshAccountDetails((LoginHeaderViewHolder) holder, sites);
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

    private void refreshAccountDetails(LoginHeaderViewHolder holder, SiteList sites) {
        if (!isAdded()) {
            return;
        }

        final boolean isWpcom = mAccountStore.hasAccessToken();

        if (isWpcom) {
            holder.updateLoggedInAsHeading(getContext(), mImageManager, mAccountStore.getAccount());
        } else if (sites.size() != 0) {
            SiteModel site = mSiteStore.getSiteByLocalId(sites.get(0).getLocalId());
            int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);

            String avatarUrl = GravatarUtils.gravatarFromEmail(site.getEmail(), avatarSz);
            String username = site.getUsername();
            String displayName = site.getDisplayName();

            holder.updateLoggedInAsHeading(getContext(), mImageManager, avatarUrl, username, displayName);
        }

        if (sites.size() == 0) {
            holder.hideSitesHeading();
//            mConnectMore.setText(R.string.connect_site);
        } else {
            holder.showSitesHeading(StringUtils.getQuantityString(
                    getActivity(), R.string.login_epilogue_mysites_one, R.string.login_epilogue_mysites_one,
                    R.string.login_epilogue_mysites_other, sites.size()));

//            mConnectMore.setText(R.string.connect_more);
        }
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
