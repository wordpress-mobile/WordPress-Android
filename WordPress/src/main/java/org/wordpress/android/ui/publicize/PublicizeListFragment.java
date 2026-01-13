package org.wordpress.android.ui.publicize;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.JetpackPoweredScreen;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.ui.ScrollableViewInitializedListener;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment;
import org.wordpress.android.ui.publicize.PublicizeListViewModel.ActionEvent;
import org.wordpress.android.ui.publicize.PublicizeListViewModel.ActionEvent.OpenServiceDetails;
import org.wordpress.android.ui.publicize.PublicizeListViewModel.UIState;
import org.wordpress.android.ui.publicize.PublicizeListViewModel.UIState.ShowTwitterDeprecationNotice;
import org.wordpress.android.ui.publicize.PublicizeTwitterDeprecationNoticeAnalyticsTracker.Source.List;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter.OnAdapterLoadedListener;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter.OnServiceClickListener;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.util.JetpackBrandingUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;

@AndroidEntryPoint
public class PublicizeListFragment extends PublicizeBaseFragment {
    public interface PublicizeButtonPrefsListener {
        void onButtonPrefsClicked();
    }

    private PublicizeButtonPrefsListener mListener;
    private SiteModel mSite;
    private PublicizeServiceAdapter mAdapter;
    private RecyclerView mRecycler;
    private TextView mEmptyView;
    private View mNestedScrollView;

    @Inject AccountStore mAccountStore;
    @Inject JetpackBrandingUtils mJetpackBrandingUtils;
    @Inject UiHelpers mUiHelpers;
    @Inject ImageManager mImageManager;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject PublicizeTwitterDeprecationNoticeAnalyticsTracker mPublicizeTwitterDeprecationNoticeAnalyticsTracker;

    PublicizeListViewModel mPublicizeListViewModel;

    public static PublicizeListFragment newInstance(@NonNull SiteModel site) {
        Bundle args = new Bundle();
        args.putSerializable(WordPress.SITE, site);

        PublicizeListFragment fragment = new PublicizeListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (getArguments() != null) {
            mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        }
        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && mRecycler.getAdapter() == null) {
            mRecycler.setAdapter(getAdapter());
        }
        getAdapter().refresh();
        setTitle(R.string.sharing);
        setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);

        if (getActivity() instanceof ScrollableViewInitializedListener) {
            ((ScrollableViewInitializedListener) getActivity()).onScrollableViewInitialized(mNestedScrollView.getId());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.publicize_list_fragment, container, false);

        mRecycler = rootView.findViewById(R.id.recycler_view);
        mEmptyView = rootView.findViewById(R.id.empty_view);
        mNestedScrollView = rootView.findViewById(R.id.publicize_list_nested_scroll_view);

        boolean isAdminOrSelfHosted = mSite.getHasCapabilityManageOptions() || !SiteUtils.isAccessedViaWPComRest(mSite);
        View manageContainer = rootView.findViewById(R.id.manage_container);
        if (isAdminOrSelfHosted) {
            manageContainer.setVisibility(View.VISIBLE);
            View manageButton = rootView.findViewById(R.id.manage_button);
            manageButton.setOnClickListener(view -> {
                if (mListener != null) {
                    mListener.onButtonPrefsClicked();
                }
            });
        } else {
            manageContainer.setVisibility(View.GONE);
        }

        if (mJetpackBrandingUtils.shouldShowJetpackBranding()) {
            final JetpackPoweredScreen screen = JetpackPoweredScreen.WithDynamicText.SHARE;
            TextView jetpackBadge = rootView.findViewById(R.id.jetpack_powered_badge);
            jetpackBadge.setVisibility(View.VISIBLE);
            jetpackBadge.setText(
                    mUiHelpers.getTextOfUiString(
                            requireContext(),
                            mJetpackBrandingUtils.getBrandingTextForScreen(screen)
                    )
            );

            if (mJetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                jetpackBadge.setOnClickListener(v -> {
                    mJetpackBrandingUtils.trackBadgeTapped(screen);
                    new JetpackPoweredBottomSheetFragment()
                            .show(requireActivity().getSupportFragmentManager(), JetpackPoweredBottomSheetFragment.TAG);
                });
            }
        }

        return rootView;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewModel();
        observeUIState();
        observeActions();
        setupTwitterDeprecationContainer();
        mPublicizeListViewModel.onSiteAvailable(mSite);
    }

    private void initViewModel() {
        mPublicizeListViewModel = new ViewModelProvider(this, mViewModelFactory).get(PublicizeListViewModel.class);
    }

    private void observeUIState() {
        mPublicizeListViewModel.getUiState().observe(getViewLifecycleOwner(), uiState -> {
            if (uiState instanceof UIState.ShowTwitterDeprecationNotice) {
                showTwitterDeprecationNotice((ShowTwitterDeprecationNotice) uiState);
            }
        });
    }

    private void observeActions() {
        mPublicizeListViewModel.getActionEvents().observe(getViewLifecycleOwner(), actionEvent -> {
            if (actionEvent instanceof ActionEvent.OpenServiceDetails) {
                onServiceClick(((OpenServiceDetails) actionEvent).getService());
            }
        });
    }

    private void setupTwitterDeprecationContainer() {
        final View rootView = getView();
        if (rootView != null) {
            final View twitterDeprecationNoticeItemContainer =
                    rootView.findViewById(R.id.publicize_twitter_deprecation_notice_item_container);
            if (twitterDeprecationNoticeItemContainer != null) {
                twitterDeprecationNoticeItemContainer.setOnClickListener(view -> {
                    mPublicizeListViewModel.onTwitterDeprecationNoticeItemClick();
                });
            }
        }
    }

    private void showTwitterDeprecationNotice(@NonNull final ShowTwitterDeprecationNotice uiState) {
        final View rootView = getView();
        if (rootView != null) {
            final View twitterContainer = rootView.findViewById(R.id.twitter_deprecation_notice_container);
            twitterContainer.setVisibility(View.VISIBLE);
            final TextView title = rootView.findViewById(R.id.publicize_twitter_deprecation_notice_header_title);
            title.setText(uiState.getTitle());
            final TextView serviceName =
                    rootView.findViewById(R.id.publicize_twitter_deprecation_notice_header_service);
            serviceName.setText(uiState.getServiceName());
            final PublicizeTwitterDeprecationNoticeWarningView warningView =
                    rootView.findViewById(R.id.publicize_twitter_deprecation_notice_header_warning);
            warningView.setTitle(getString(uiState.getTitle()));
            warningView.setDescription(getString(uiState.getDescription()), getString(uiState.getFindOutMore()),
                    () -> {
                        mPublicizeTwitterDeprecationNoticeAnalyticsTracker.trackTwitterNoticeLinkTapped(List.INSTANCE);
                        WPWebViewActivity.openURL(getActivity(), uiState.getFindOutMoreUrl());
                        return Unit.INSTANCE;
                    });
            final ImageView icon = rootView.findViewById(R.id.publicize_twitter_deprecation_notice_header_icon);
            mImageManager.load(icon, ImageType.AVATAR_WITH_BACKGROUND, uiState.getIconUrl());
            final TextView connectedUser = rootView.findViewById(R.id.publicize_twitter_deprecation_notice_header_user);
            connectedUser.setText(uiState.getConnectedUser());
        }
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);

        if (activity instanceof PublicizeButtonPrefsListener) {
            mListener = (PublicizeButtonPrefsListener) activity;
        } else {
            throw new RuntimeException(activity.toString() + " must implement PublicizeButtonPrefsListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private final OnAdapterLoadedListener mAdapterLoadedListener = new OnAdapterLoadedListener() {
        @Override
        public void onAdapterLoaded(boolean isEmpty) {
            if (!isAdded()) {
                return;
            }

            if (isEmpty) {
                if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                    mEmptyView.setText(R.string.no_network_title);
                } else {
                    mEmptyView.setText(R.string.loading);
                }
            }
            mEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    };

    private PublicizeServiceAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new PublicizeServiceAdapter(
                    getActivity(),
                    mSite.getSiteId(),
                    mAccountStore.getAccount().getUserId());
            mAdapter.setOnAdapterLoadedListener(mAdapterLoadedListener);
            if (getActivity() instanceof OnServiceClickListener) {
                mAdapter.setOnServiceClickListener(this::onServiceClick);
            }
        }
        return mAdapter;
    }

    private void onServiceClick(@NonNull final PublicizeService service) {
        ((OnServiceClickListener) getActivity()).onServiceClicked(service);
    }

    void reload() {
        getAdapter().refresh();
    }
}
