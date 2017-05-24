package org.wordpress.android.ui.publicize;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;
import org.wordpress.android.ui.publicize.adapters.PublicizeConnectionAdapter;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.RecyclerItemDecoration;
import org.wordpress.android.widgets.WPNetworkImageView;

public class PublicizeDetailFragment extends PublicizeBaseFragment implements PublicizeConnectionAdapter.OnAdapterLoadedListener {
    private int mSiteId;
    private String mServiceId;
    private long mCurrentUserId;

    private PublicizeService mService;

    private ConnectButton mConnectBtn;
    private RecyclerView mRecycler;
    private ViewGroup mLayoutConnections;

    public static PublicizeDetailFragment newInstance(int siteId, PublicizeService service, long currentUserId) {
        Bundle args = new Bundle();
        args.putInt(PublicizeConstants.ARG_SITE_ID, siteId);
        args.putString(PublicizeConstants.ARG_SERVICE_ID, service.getId());
        args.putLong(PublicizeConstants.ARG_USER_ID, currentUserId);
        PublicizeDetailFragment fragment = new PublicizeDetailFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null) {
            mSiteId = args.getInt(PublicizeConstants.ARG_SITE_ID);
            mServiceId = args.getString(PublicizeConstants.ARG_SERVICE_ID);
            mCurrentUserId = args.getLong(PublicizeConstants.ARG_USER_ID);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mSiteId = savedInstanceState.getInt(PublicizeConstants.ARG_SITE_ID);
            mServiceId = savedInstanceState.getString(PublicizeConstants.ARG_SERVICE_ID);
            mCurrentUserId = savedInstanceState.getLong(PublicizeConstants.ARG_USER_ID);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PublicizeConstants.ARG_SITE_ID, mSiteId);
        outState.putString(PublicizeConstants.ARG_SERVICE_ID, mServiceId);
        outState.putLong(PublicizeConstants.ARG_USER_ID, mCurrentUserId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.publicize_detail_fragment, container, false);

        mConnectBtn = (ConnectButton) rootView.findViewById(R.id.button_connect);
        mLayoutConnections = (ViewGroup) rootView.findViewById(R.id.layout_connections);
        mRecycler = (RecyclerView) rootView.findViewById(R.id.recycler_view);

        int spacingHorizontal = 0;
        int spacingVertical = DisplayUtils.dpToPx(getActivity(), 1);
        mRecycler.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

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

        TextView txtService = (TextView) getView().findViewById(R.id.text_service);
        TextView txtDescription = (TextView) getView().findViewById(R.id.text_description);
        WPNetworkImageView imgIcon = (WPNetworkImageView) getView().findViewById(R.id.image_icon);

        txtService.setText(mService.getLabel());
        txtDescription.setText(mService.getDescription());

        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
        String iconUrl = PhotonUtils.getPhotonImageUrl(mService.getIconUrl(), avatarSz, avatarSz);
        imgIcon.setImageUrl(iconUrl, WPNetworkImageView.ImageType.BLAVATAR);

        PublicizeConnectionAdapter adapter =
                new PublicizeConnectionAdapter(getActivity(), mSiteId, mServiceId, mCurrentUserId);
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

        mLayoutConnections.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

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
