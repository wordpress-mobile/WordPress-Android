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
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.login.LoginBaseFormFragment;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Click;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step;
import org.wordpress.android.ui.main.SitePickerAdapter;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteList;
import org.wordpress.android.ui.main.SitePickerAdapter.SitePickerMode;
import org.wordpress.android.ui.main.SitePickerAdapter.ViewHolderHandler;
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

    private SitePickerAdapter mAdapter;
    private boolean mDoLoginUpdate;
    private boolean mShowAndReturn;
    private ArrayList<Integer> mOldSitesIds;

    private LoginEpilogueListener mLoginEpilogueListener;

    @Inject ImageManager mImageManager;
    @Inject UnifiedLoginTracker mUnifiedLoginTracker;

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
        mSitesList = rootView.findViewById(R.id.recycler_view);
        mSitesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSitesList.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mSitesList.setItemAnimator(null);
        mSitesList.setAdapter(getAdapter());
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        primaryButton.setOnClickListener(v -> {
            mUnifiedLoginTracker.trackClick(Click.CONTINUE);
            if (mLoginEpilogueListener != null) {
                mLoginEpilogueListener.onContinue();
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
            mUnifiedLoginTracker.track(Step.SUCCESS);
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
                        mSitesList.post(() -> {
                            if (!isAdded()) {
                                return;
                            }

                            if (mSitesList.computeVerticalScrollRange() > mSitesList.getHeight()) {
                                mBottomShadow.setVisibility(View.VISIBLE);
                            } else {
                                mBottomShadow.setVisibility(View.GONE);
                            }
                        });
                    }
                },
                new SitePickerAdapter.ViewHolderHandler<LoginHeaderViewHolder>() {
                    @Override
                    public LoginHeaderViewHolder onCreateViewHolder(LayoutInflater layoutInflater, ViewGroup parent,
                                                                    boolean attachToRoot) {
                        return new LoginHeaderViewHolder(
                                layoutInflater.inflate(R.layout.login_epilogue_header, parent, false));
                    }

                    @Override
                    public void onBindViewHolder(LoginHeaderViewHolder holder, SiteList sites) {
                        bindHeaderViewHolder(holder, sites);
                    }
                },
                new ViewHolderHandler<LoginFooterViewHolder>() {
                    @Override
                    public LoginFooterViewHolder onCreateViewHolder(LayoutInflater layoutInflater, ViewGroup parent,
                                                                    boolean attachToRoot) {
                        return new LoginFooterViewHolder(
                                layoutInflater.inflate(R.layout.login_epilogue_footer, parent, false));
                    }

                    @Override
                    public void onBindViewHolder(LoginFooterViewHolder holder, SiteList sites) {
                        bindFooterViewHolder(holder, sites);
                    }
                }, mOldSitesIds, SitePickerMode.DEFAULT_MODE);
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

    private void bindHeaderViewHolder(LoginHeaderViewHolder holder, SiteList sites) {
        if (!isAdded()) {
            return;
        }

        final boolean isWpcom = mAccountStore.hasAccessToken();
        final boolean hasSites = sites.size() != 0;

        if (isWpcom) {
            final AccountModel account = mAccountStore.getAccount();
            holder.updateLoggedInAsHeading(getContext(), mImageManager, account);
        } else if (hasSites) {
            final SiteModel site = mSiteStore.getSiteByLocalId(sites.get(0).getLocalId());
            holder.updateLoggedInAsHeading(getContext(), mImageManager, site);
        }

        if (hasSites) {
            holder.showSitesHeading(StringUtils.getQuantityString(
                    getActivity(), R.string.login_epilogue_mysites_one, R.string.login_epilogue_mysites_one,
                    R.string.login_epilogue_mysites_other, sites.size()));
        } else {
            holder.hideSitesHeading();
        }
    }

    private void bindFooterViewHolder(LoginFooterViewHolder holder, SiteList sites) {
        holder.itemView.setVisibility(mShowAndReturn ? View.GONE : View.VISIBLE);
        holder.itemView.setOnClickListener(v -> {
            if (mLoginEpilogueListener != null) {
                mUnifiedLoginTracker.trackClick(Click.CONNECT_SITE);
                mLoginEpilogueListener.onConnectAnotherSite();
            }
        });
        if (sites.size() == 0) {
            holder.bindText(getString(R.string.connect_site));
        } else {
            holder.bindText(getString(R.string.connect_more));
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
