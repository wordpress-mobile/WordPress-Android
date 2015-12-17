package org.wordpress.android.ui.publicize.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.models.PublicizeConnectionList;
import org.wordpress.android.ui.publicize.ConnectButton;
import org.wordpress.android.ui.publicize.PublicizeConstants;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class PublicizeConnectionAdapter extends RecyclerView.Adapter<PublicizeConnectionAdapter.ConnectionViewHolder> {

    private final PublicizeConnectionList mConnections = new PublicizeConnectionList();

    private final int mSiteId;
    private final int mAvatarSz;
    private final long mCurrentUserId;
    private final String mServiceId;

    public PublicizeConnectionAdapter(Context context, int siteId, String serviceId) {
        super();
        mSiteId = siteId;
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_extra_small);
        mServiceId = StringUtils.notNullStr(serviceId);
        mCurrentUserId = AccountHelper.getDefaultAccount().getUserId();
        setHasStableIds(true);
    }

    public void refresh() {
        PublicizeConnectionList connections = PublicizeTable.getConnectionsForSiteAndService(mSiteId, mServiceId);
        if (!mConnections.isSameAs(connections)) {
            mConnections.clear();
            mConnections.addAll(connections);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return mConnections.size();
    }

    private boolean isEmpty() {
        return (getItemCount() == 0);
    }

    @Override
    public long getItemId(int position) {
        return mConnections.get(position).connectionId;
    }

    @Override
    public ConnectionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.publicize_listitem_connection, parent, false);
        return new ConnectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ConnectionViewHolder holder, int position) {
        PublicizeConnection connection = mConnections.get(position);

        holder.txtUser.setText(connection.getExternalDisplayName());

        String avatarUrl = PhotonUtils.getPhotonImageUrl(connection.getExternalProfilePictureUrl(), mAvatarSz, mAvatarSz);
        holder.imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.BLAVATAR);

        holder.btnConnect.setAction(PublicizeConstants.ConnectAction.DISCONNECT);
    }

    class ConnectionViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtUser;
        private final ConnectButton btnConnect;
        private final WPNetworkImageView imgAvatar;

        public ConnectionViewHolder(View view) {
            super(view);
            txtUser = (TextView) view.findViewById(R.id.text_user);
            imgAvatar = (WPNetworkImageView) view.findViewById(R.id.image_avatar);
            btnConnect = (ConnectButton) view.findViewById(R.id.button_connect);
        }
    }

}
