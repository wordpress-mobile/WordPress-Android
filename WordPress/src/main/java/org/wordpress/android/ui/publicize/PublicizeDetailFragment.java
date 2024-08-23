package org.wordpress.android.ui.publicize;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.models.PublicizeService.Status;
import org.wordpress.android.ui.ScrollableViewInitializedListener;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;
import org.wordpress.android.ui.publicize.PublicizeTwitterDeprecationNoticeAnalyticsTracker.Source.Detail;
import org.wordpress.android.ui.publicize.adapters.PublicizeConnectionAdapter;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

import kotlin.Unit;

import static org.wordpress.android.ui.publicize.PublicizeTwitterDeprecationNoticeConstantsKt.TWITTER_DEPRECATION_FIND_OUT_MORE_URL;

public class PublicizeDetailFragment extends PublicizeBaseFragment
        implements PublicizeConnectionAdapter.OnAdapterLoadedListener {
    private SiteModel mSite;
    private String mServiceId;

    private PublicizeService mService;

    private ConnectButton mConnectBtn;
    private RecyclerView mRecycler;
    private View mConnectionsContainer;
    private ViewGroup mServiceContainer;
    private View mNestedScrollView;
    private PublicizeTwitterDeprecationNoticeWarningView mTwitterDeprecationNoticeWarningContainer;
    private TextView mConnectedAccounts;

    @Inject AccountStore mAccountStore;
    @Inject PublicizeTwitterDeprecationNoticeAnalyticsTracker mPublicizeTwitterDeprecationNoticeAnalyticsTracker;

    public static PublicizeDetailFragment newInstance(@NonNull SiteModel site, @NonNull PublicizeService service) {
        Bundle args = new Bundle();
        args.putSerializable(WordPress.SITE, site);
        args.putString(PublicizeConstants.ARG_SERVICE_ID, service.getId());

        PublicizeDetailFragment fragment = new PublicizeDetailFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null) {
            mSite = (SiteModel) args.getSerializable(WordPress.SITE);
            mServiceId = args.getString(PublicizeConstants.ARG_SERVICE_ID);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mServiceId = savedInstanceState.getString(PublicizeConstants.ARG_SERVICE_ID);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putString(PublicizeConstants.ARG_SERVICE_ID, mServiceId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.publicize_detail_fragment, container, false);

        mConnectionsContainer = rootView.findViewById(R.id.connections_container);
        mServiceContainer = rootView.findViewById(R.id.service_container);
        mConnectBtn = mServiceContainer.findViewById(R.id.button_connect);
        mRecycler = rootView.findViewById(R.id.recycler_view);
        mNestedScrollView = rootView.findViewById(R.id.publicize_details_nested_scroll_View);
        mTwitterDeprecationNoticeWarningContainer =
                rootView.findViewById(R.id.publicize_twitter_deprecation_notice_detail_warning);
        mConnectedAccounts = rootView.findViewById(R.id.connected_accounts);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
        setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        if (getActivity() instanceof ScrollableViewInitializedListener) {
            ((ScrollableViewInitializedListener) getActivity()).onScrollableViewInitialized(mNestedScrollView.getId());
        }
    }

    public void loadData() {
        if (!isAdded()) {
            return;
        }

        mService = PublicizeTable.getService(mServiceId);
        if (mService == null) {
            ToastUtils.showToast(getActivity(), R.string.error_generic);
            return;
        }

        setTitle(mService.getLabel());

        // disable the ability to add another G+ or Twitter connection
        if (isGooglePlus() || isTwitterUnsupported()) {
            mServiceContainer.setVisibility(View.GONE);
            if (isTwitterUnsupported()) {
                showTwitterDeprecationContainer();
            } else {
                mTwitterDeprecationNoticeWarningContainer.setVisibility(View.GONE);
            }
        } else {
            mTwitterDeprecationNoticeWarningContainer.setVisibility(View.GONE);
            mConnectedAccounts.setVisibility(View.VISIBLE);
            mServiceContainer.setVisibility(View.VISIBLE);
            String serviceLabel = String.format(getString(R.string.connection_service_label), mService.getLabel());
            TextView txtService = mServiceContainer.findViewById(R.id.text_service);
            txtService.setText(serviceLabel);

            String description = String.format(getString(R.string.connection_service_description), mService.getLabel());
            TextView txtDescription = mServiceContainer.findViewById(R.id.text_description);
            txtDescription.setText(description);
        }

        long currentUserId = mAccountStore.getAccount().getUserId();
        PublicizeConnectionAdapter adapter = new PublicizeConnectionAdapter(
                getActivity(), mSite.getSiteId(), mService, currentUserId);
        adapter.setOnPublicizeActionListener(getOnPublicizeActionListener());
        adapter.setOnAdapterLoadedListener(this);

        mRecycler.setAdapter(adapter);
        adapter.refresh();
    }

    private void showTwitterDeprecationContainer() {
        mConnectedAccounts.setVisibility(View.GONE);
        mTwitterDeprecationNoticeWarningContainer.setVisibility(View.VISIBLE);
        mTwitterDeprecationNoticeWarningContainer.setTitle(getString(R.string.connected_accounts_label));
        mTwitterDeprecationNoticeWarningContainer.setDescription(
                getString(R.string.sharing_twitter_deprecation_notice_description),
                getString(R.string.sharing_twitter_deprecation_notice_find_out_more),
                () -> {
                    mPublicizeTwitterDeprecationNoticeAnalyticsTracker.trackTwitterNoticeLinkTapped(Detail.INSTANCE);
                    WPWebViewActivity.openURL(getActivity(), TWITTER_DEPRECATION_FIND_OUT_MORE_URL);
                    return Unit.INSTANCE;
                });
    }

    private boolean isGooglePlus() {
        return mService.getId().equals(PublicizeConstants.GOOGLE_PLUS_ID);
    }

    private boolean isTwitterUnsupported() {
        return mService.getId().equals(PublicizeService.TWITTER_SERVICE_ID)
               && mService.getStatus() == Status.UNSUPPORTED;
    }

    private boolean hasOnPublicizeActionListener() {
        return getOnPublicizeActionListener() != null;
    }

    private PublicizeActions.OnPublicizeActionListener getOnPublicizeActionListener() {
        if (getActivity() instanceof PublicizeActions.OnPublicizeActionListener) {
            return (PublicizeActions.OnPublicizeActionListener) getActivity();
        }
        return null;
    }

    @Override
    public void onAdapterLoaded(boolean isEmpty) {
        if (!isAdded()) {
            return;
        }

        mConnectionsContainer.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (hasOnPublicizeActionListener()) {
            if (isEmpty) {
                mConnectBtn.setAction(ConnectAction.CONNECT);
            } else {
                mConnectBtn.setAction(ConnectAction.CONNECT_ANOTHER_ACCOUNT);
            }
            mConnectBtn.setOnClickListener(v -> getOnPublicizeActionListener().onRequestConnect(mService));
        }
    }
}
