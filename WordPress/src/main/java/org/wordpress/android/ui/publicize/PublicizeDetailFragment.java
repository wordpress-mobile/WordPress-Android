package org.wordpress.android.ui.publicize;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.ui.publicize.adapters.PublicizeConnectionAdapter;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.RecyclerItemDecoration;
import org.wordpress.android.widgets.WPNetworkImageView;

import de.greenrobot.event.EventBus;

public class PublicizeDetailFragment extends PublicizeBaseFragment implements PublicizeConnectionAdapter.OnAdapterLoadedListener {

    private int mSiteId;
    private String mServiceId;

    private PublicizeService mService;

    private ConnectButton mConnectBtn;
    private RecyclerView mRecycler;
    private ViewGroup mLayoutConnections;

    public static PublicizeDetailFragment newInstance(int siteId, PublicizeService service) {
        Bundle args = new Bundle();
        args.putInt(PublicizeConstants.ARG_SITE_ID, siteId);
        args.putString(PublicizeConstants.ARG_SERVICE_ID, service.getId());
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
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mSiteId = savedInstanceState.getInt(PublicizeConstants.ARG_SITE_ID);
            mServiceId = savedInstanceState.getString(PublicizeConstants.ARG_SERVICE_ID);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PublicizeConstants.ARG_SITE_ID, mSiteId);
        outState.putString(PublicizeConstants.ARG_SERVICE_ID, mServiceId);
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
        setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
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

        PublicizeConnectionAdapter adapter = new PublicizeConnectionAdapter(getActivity(), mSiteId, mServiceId);
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
                    try {
                        JSONObject object = new JSONObject("{\"connections\":[{\"ID\":14460414,\"user_ID\":85830968,\"type\":\"publicize\",\"service\":\"facebook\",\"label\":\"Facebook\",\"issued\":\"2016-05-15 23:27:10\",\"expires\":\"2016-07-14 16:47:41\",\"external_ID\":\"122653424811017\",\"external_name\":null,\"external_display\":\"Jason Lee\",\"external_profile_picture\":\"https:\\/\\/scontent.xx.fbcdn.net\\/v\\/t1.0-1\\/c47.0.160.160\\/p160x160\\/10354686_10150004552801856_220367501106153455_n.jpg?oh=b5df82644cdd221e1da17017092c51f5&oe=57E04C49\",\"additional_external_users\":[],\"status\":\"ok\",\"refresh_URL\":\"https:\\/\\/public-api.wordpress.com\\/connect\\/?action=request&kr_nonce=a685796cd6&nonce=1fb9b2d834&refresh=1&for=connect&service=facebook&kr_blog_nonce=dab5dee2b5&magic=keyring&blog=90298630\",\"sites\":[\"90298630\"],\"meta\":{\"links\":{\"self\":\"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/me\\/keyring-connections\\/14460414\",\"help\":\"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/me\\/keyring-connections\\/14460414\\/help\",\"service\":\"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/meta\\/external-services\\/facebook\",\"publicize_connections\":\"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/me\\/publicize-connections\\/?keyring_connection_ID=14460414\"}}},{\"ID\":14477513,\"user_ID\":85830968,\"type\":\"publicize\",\"service\":\"facebook\",\"label\":\"Facebook\",\"issued\":\"2016-05-17 19:21:32\",\"expires\":\"2016-08-25 17:15:17\",\"external_ID\":\"123585121381948\",\"external_name\":null,\"external_display\":\"Joseph Park\",\"external_profile_picture\":\"https:\\/\\/scontent.xx.fbcdn.net\\/v\\/t1.0-1\\/c47.0.160.160\\/p160x160\\/1379841_10150004552801901_469209496895221757_n.jpg?oh=b2b810118540c28b9a9766b47aed25e8&oe=57F1ED56\",\"additional_external_users\":[],\"status\":\"ok\",\"refresh_URL\":\"https:\\/\\/public-api.wordpress.com\\/connect\\/?action=request&kr_nonce=a685796cd6&nonce=1fb9b2d834&refresh=1&for=connect&service=facebook&kr_blog_nonce=dab5dee2b5&magic=keyring&blog=90298630\",\"sites\":[],\"meta\":{\"links\":{\"self\":\"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/me\\/keyring-connections\\/14477513\",\"help\":\"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/me\\/keyring-connections\\/14477513\\/help\",\"service\":\"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/meta\\/external-services\\/facebook\",\"publicize_connections\":\"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/me\\/publicize-connections\\/?keyring_connection_ID=14477513\"}}},{\"ID\":14359331,\"user_ID\":85830968,\"type\":\"publicize\",\"service\":\"facebook\",\"label\":\"Facebook\",\"issued\":\"2016-05-03 21:43:25\",\"expires\":\"0000-00-00 00:00:00\",\"external_ID\":\"760048377\",\"external_name\":null,\"external_display\":\"Will Kwon\",\"external_profile_picture\":\"https:\\/\\/scontent.xx.fbcdn.net\\/v\\/t1.0-1\\/c0.11.160.160\\/p160x160\\/12140837_10154338963603378_3978128539399730629_n.jpg?oh=f9fc9fbc4d7b7db211e9eb36b3b073b1&oe=57FC0952\",\"additional_external_users\":[],\"status\":\"ok\",\"refresh_URL\":\"https:\\/\\/public-api.wordpress.com\\/connect\\/?action=request&kr_nonce=a685796cd6&nonce=1fb9b2d834&refresh=1&for=connect&service=facebook&kr_blog_nonce=dab5dee2b5&magic=keyring&blog=90298630\",\"sites\":[\"106611662\"],\"meta\":{\"links\":{\"self\":\"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/me\\/keyring-connections\\/14359331\",\"help\":\"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/me\\/keyring-connections\\/14359331\\/help\",\"service\":\"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/meta\\/external-services\\/facebook\",\"publicize_connections\":\"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/me\\/publicize-connections\\/?keyring_connection_ID=14359331\"}}}]}");
                        EventBus.getDefault().post(new PublicizeEvents.ConnectionChooserRequired(object));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    
//                    getOnPublicizeActionListener().onRequestConnect(mService);
                }
            });
        }
    }
}
