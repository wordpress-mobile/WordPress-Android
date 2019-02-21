package org.wordpress.android.ui.publicize;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;
import org.wordpress.android.ui.publicize.adapters.PublicizeConnectionAdapter;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

public class PublicizeDetailFragment extends PublicizeBaseFragment
        implements PublicizeConnectionAdapter.OnAdapterLoadedListener {
    public static final String FACEBOOK_SHARING_CHANGE_BLOG_POST =
            "https://en.blog.wordpress.com/2018/07/23/sharing-options-from-wordpress-com-to-facebook-are-changing/";
    private SiteModel mSite;
    private String mServiceId;

    private PublicizeService mService;

    private ConnectButton mConnectBtn;
    private RecyclerView mRecycler;
    private View mConnectionsCardView;
    private ViewGroup mServiceCardView;

    @Inject AccountStore mAccountStore;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mServiceId = savedInstanceState.getString(PublicizeConstants.ARG_SERVICE_ID);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putString(PublicizeConstants.ARG_SERVICE_ID, mServiceId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.publicize_detail_fragment, container, false);

        mConnectionsCardView = rootView.findViewById(R.id.card_view_connections);
        mServiceCardView = (ViewGroup) rootView.findViewById(R.id.card_view_service);
        mConnectBtn = (ConnectButton) mServiceCardView.findViewById(R.id.button_connect);
        mRecycler = (RecyclerView) rootView.findViewById(R.id.recycler_view);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
        setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
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

        // disable the ability to add another G+ connection
        if (isGooglePlus()) {
            mServiceCardView.setVisibility(View.GONE);
        } else {
            String serviceLabel = String.format(getString(R.string.connection_service_label), mService.getLabel());
            TextView txtService = (TextView) mServiceCardView.findViewById(R.id.text_service);
            txtService.setText(serviceLabel);

            String description = String.format(getString(R.string.connection_service_description), mService.getLabel());
            TextView txtDescription = (TextView) mServiceCardView.findViewById(R.id.text_description);
            txtDescription.setText(description);

            if (isFacebook()) {
                showFacebookWarning();
            }
        }

        long currentUserId = mAccountStore.getAccount().getUserId();
        PublicizeConnectionAdapter adapter = new PublicizeConnectionAdapter(
                getActivity(), mSite.getSiteId(), mService, currentUserId);
        adapter.setOnPublicizeActionListener(getOnPublicizeActionListener());
        adapter.setOnAdapterLoadedListener(this);

        mRecycler.setAdapter(adapter);
        adapter.refresh();
    }

    private boolean isGooglePlus() {
        return mService.getId().equals(PublicizeConstants.GOOGLE_PLUS_ID);
    }

    private boolean isFacebook() {
        return mService.getId().equals(PublicizeConstants.FACEBOOK_ID);
    }

    private void showFacebookWarning() {
        String noticeText = getString(R.string.connection_service_facebook_notice);
        TextView txtNotice = (TextView) mServiceCardView.findViewById(R.id.text_description_notice);
        txtNotice.setText(noticeText);
        txtNotice.setVisibility(View.VISIBLE);

        TextView learnMoreButton = (TextView) mServiceCardView.findViewById(R.id.learn_more_button);
        learnMoreButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                WPWebViewActivity.openURL(getActivity(),
                        FACEBOOK_SHARING_CHANGE_BLOG_POST);
            }
        });
        learnMoreButton.setVisibility(View.VISIBLE);
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

        mConnectionsCardView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (hasOnPublicizeActionListener()) {
            if (isEmpty) {
                mConnectBtn.setAction(ConnectAction.CONNECT);
            } else {
                mConnectBtn.setAction(ConnectAction.CONNECT_ANOTHER_ACCOUNT);
            }
            mConnectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getOnPublicizeActionListener().onRequestConnect(mService);
                }
            });
        }
    }
}
