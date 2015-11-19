package org.wordpress.android.ui.publicize;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class PublicizeDetailFragment extends Fragment {

    public interface OnPublicizeActionListener {
        void onRequestConnect(PublicizeService service);
        void onRequestDisconnect(PublicizeConnection connection);
    }

    private int mSiteId;
    private int mConnectionId;
    private String mServiceId;

    private PublicizeService mService;
    private PublicizeConnection mConnection;

    private ConnectButton mConnectButton;
    private OnPublicizeActionListener mActionListener;

    public static PublicizeDetailFragment newInstance(
            int siteId,
            PublicizeService service,
            PublicizeConnection connection) {
        Bundle args = new Bundle();
        args.putInt(PublicizeConstants.ARG_SITE_ID, siteId);
        args.putString(PublicizeConstants.ARG_SERVICE_ID, service.getId());
        if (connection != null) {
            args.putInt(PublicizeConstants.ARG_CONNECTION_ID, connection.connectionId);
        }

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
            mConnectionId = args.getInt(PublicizeConstants.ARG_CONNECTION_ID);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mSiteId = savedInstanceState.getInt(PublicizeConstants.ARG_SITE_ID);
            mServiceId = savedInstanceState.getString(PublicizeConstants.ARG_SERVICE_ID);
            mConnectionId = savedInstanceState.getInt(PublicizeConstants.ARG_CONNECTION_ID);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PublicizeConstants.ARG_SITE_ID, mSiteId);
        outState.putString(PublicizeConstants.ARG_SERVICE_ID, mServiceId);
        outState.putInt(PublicizeConstants.ARG_CONNECTION_ID, mConnectionId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.publicize_detail_fragment, container, false);
        mConnectButton = (ConnectButton) rootView.findViewById(R.id.button_connect);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnPublicizeActionListener) {
            mActionListener = (OnPublicizeActionListener) activity;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getData();
    }

    void getData() {
        new Thread() {
            @Override
            public void run() {
                // service should always exist, but connection will be null if no connection
                // has been enabled for this service
                mService = PublicizeTable.getService(mServiceId);
                mConnection = PublicizeTable.getConnection(mConnectionId);

                if (isAdded()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isAdded()) {
                                showData();
                            }
                        }
                    });
                }
            }
        }.start();
    }

    private void showData() {
        if (!isAdded()) return;

        TextView txtService = (TextView) getView().findViewById(R.id.text_service);
        TextView txtDescription = (TextView) getView().findViewById(R.id.text_description);
        WPNetworkImageView imgIcon = (WPNetworkImageView) getView().findViewById(R.id.image_icon);

        txtService.setText(mService.getLabel());
        txtDescription.setText(mService.getDescription());

        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
        String iconUrl = PhotonUtils.getPhotonImageUrl(mService.getIconUrl(), avatarSz, avatarSz);
        imgIcon.setImageUrl(iconUrl, WPNetworkImageView.ImageType.BLAVATAR);

        ConnectAction action;
        if (mConnection == null) {
            action = ConnectAction.CONNECT;
        } else if (mConnection.getStatusEnum() == PublicizeConnection.ConnectStatus.BROKEN) {
            action = ConnectAction.RECONNECT;
        } else {
            action = ConnectAction.DISCONNECT;
        }
        mConnectButton.setAction(action);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doConnectBtnClick();
            }
        });
    }

    private void doConnectBtnClick() {
        if (mActionListener == null) return;

        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        switch (mConnectButton.getAction()) {
            case CONNECT:
                mActionListener.onRequestConnect(mService);
                break;
            case DISCONNECT:
                mActionListener.onRequestDisconnect(mConnection);
                break;
            case RECONNECT:
                // TODO
                break;
        }
    }
}
