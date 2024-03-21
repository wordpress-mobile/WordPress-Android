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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.login.LoginBaseFormFragment;
import org.wordpress.android.ui.accounts.LoginEpilogueViewModel;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Click;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step;
import org.wordpress.android.ui.main.ChooseSiteAdapter;
import org.wordpress.android.util.BuildConfigWrapper;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.image.ImageManager;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;

@AndroidEntryPoint
public class LoginEpilogueFragment extends LoginBaseFormFragment<LoginEpilogueListener> {
    public static final String TAG = "login_epilogue_fragment_tag";

    private static final String ARG_DO_LOGIN_UPDATE = "ARG_DO_LOGIN_UPDATE";
    private static final String ARG_SHOW_AND_RETURN = "ARG_SHOW_AND_RETURN";
    private static final String ARG_OLD_SITES_IDS = "ARG_OLD_SITES_IDS";

    private RecyclerView mSitesList;
    @Nullable private View mBottomShadow;

    private ChooseSiteAdapter mAdapter = new ChooseSiteAdapter();
    private boolean mDoLoginUpdate;
    private boolean mShowAndReturn;

    private LoginEpilogueListener mLoginEpilogueListener;

    @Inject ImageManager mImageManager;
    @Inject UnifiedLoginTracker mUnifiedLoginTracker;
    @Inject BuildConfigWrapper mBuildConfigWrapper;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject LoginEpilogueViewModel mParentViewModel;

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
        return (ViewGroup) inflater.inflate(loginEpilogueScreenResource(), container, false);
    }

    @LayoutRes
    private int loginEpilogueScreenResource() {
        if (isNewLoginEpilogueScreenEnabled()) {
            return R.layout.login_epilogue_screen_new_expanded;
        } else {
            return R.layout.login_epilogue_screen;
        }
    }

    private boolean isNewLoginEpilogueScreenEnabled() {
        return mBuildConfigWrapper.isSiteCreationEnabled()
               && !mShowAndReturn;
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        mBottomShadow = rootView.findViewById(R.id.bottom_shadow);
        mSitesList = rootView.findViewById(R.id.recycler_view);
        mSitesList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        mSitesList.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mSitesList.setItemAnimator(null);
        mSitesList.setAdapter(mAdapter);
        setOnSiteClickListener();
    }

    @Override
    protected void setupBottomButton(Button button) {
        button.setOnClickListener(v -> {
            if (mLoginEpilogueListener != null) {
                if (isNewLoginEpilogueScreenEnabled()) {
                    AnalyticsTracker.track(Stat.LOGIN_EPILOGUE_CREATE_NEW_SITE_TAPPED);
                    mUnifiedLoginTracker.trackClick(Click.CREATE_NEW_SITE);
                    mLoginEpilogueListener.onCreateNewSite();
                } else {
                    mUnifiedLoginTracker.trackClick(Click.CONTINUE);
                    mLoginEpilogueListener.onContinue();
                }
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDoLoginUpdate = requireArguments().getBoolean(ARG_DO_LOGIN_UPDATE, false);
        mShowAndReturn = requireArguments().getBoolean(ARG_SHOW_AND_RETURN, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewModel();
        mParentViewModel.getSites().observe(requireActivity(), sites -> {
            if (mBottomShadow != null) {
                if (mSitesList.computeVerticalScrollRange() > mSitesList.getHeight()) {
                    mBottomShadow.setVisibility(View.VISIBLE);
                } else {
                    mBottomShadow.setVisibility(View.GONE);
                }
            }
            mAdapter.setSites(sites);
            mParentViewModel.onSiteListLoaded();
        });
        mParentViewModel.loadSites();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_EPILOGUE_VIEWED);
            mUnifiedLoginTracker.track(Step.SUCCESS);
        }
    }

    private void initViewModel() {
        mParentViewModel = new ViewModelProvider(requireActivity(), mViewModelFactory)
                .get(LoginEpilogueViewModel.class);
    }

    private void setOnSiteClickListener() {
        if (isNewLoginEpilogueScreenEnabled()) {
            mAdapter.setOnSiteClicked((site) -> {
                AnalyticsTracker.track(Stat.LOGIN_EPILOGUE_CHOOSE_SITE_TAPPED);
                mUnifiedLoginTracker.trackClick(Click.CHOOSE_SITE);
                mLoginEpilogueListener.onSiteClick(site.getLocalId());
                return Unit.INSTANCE;
            });
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

        if (mDoLoginUpdate) {
            // when from magiclink, we need to complete the login process here (update account and settings)
            doFinishLogin();
        }
        mParentViewModel.onLoginEpilogueResume(mDoLoginUpdate);
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
        mParentViewModel.onLoginFinished(mDoLoginUpdate);
    }

    @Override
    protected boolean isJetpackAppLogin() {
        return mDoLoginUpdate && mBuildConfigWrapper.isJetpackApp();
    }
}
