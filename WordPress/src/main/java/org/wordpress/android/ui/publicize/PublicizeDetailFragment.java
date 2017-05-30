package org.wordpress.android.ui.publicize;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;
import org.wordpress.android.ui.publicize.adapters.PublicizeConnectionAdapter;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

public class PublicizeDetailFragment extends PublicizeBaseFragment implements PublicizeConnectionAdapter.OnAdapterLoadedListener {
    private SiteModel mSite;
    private String mServiceId;

    private PublicizeService mService;

    private ConnectButton mConnectBtn;
    private RecyclerView mRecycler;
    private View mConnectionsCardView;

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

        mConnectBtn = (ConnectButton) rootView.findViewById(R.id.button_connect);
        mConnectionsCardView = rootView.findViewById(R.id.card_view_connections);
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
        if (!isAdded()) return;

        mService = PublicizeTable.getService(mServiceId);
        if (mService == null) {
            ToastUtils.showToast(getActivity(), R.string.error_generic);
            return;
        }

        setTitle(mService.getLabel());

        String serviceLabel = String.format(getString(R.string.connection_service_label), mService.getLabel());
        TextView txtService = (TextView) getView().findViewById(R.id.text_service);
        txtService.setText(serviceLabel);

        String description = String.format(getString(R.string.connection_service_description), mService.getLabel());
        TextView txtDescription = (TextView) getView().findViewById(R.id.text_description);
        txtDescription.setText(description);

        long currentUserId = mAccountStore.getAccount().getUserId();
        PublicizeConnectionAdapter adapter = new PublicizeConnectionAdapter(
                getActivity(), mSite.getSiteId(), mServiceId, currentUserId);
        adapter.setOnPublicizeActionListener(getOnPublicizeActionListener());
        adapter.setOnAdapterLoadedListener(this);

        mRecycler.setAdapter(adapter);
        adapter.refresh();
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
        if (!isAdded()) return;

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
