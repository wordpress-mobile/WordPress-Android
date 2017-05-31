package org.wordpress.android.ui.publicize.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.models.PublicizeConnectionList;
import org.wordpress.android.ui.publicize.ConnectButton;
import org.wordpress.android.ui.publicize.PublicizeActions;
import org.wordpress.android.ui.publicize.PublicizeConstants;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class PublicizeConnectionAdapter extends RecyclerView.Adapter<PublicizeConnectionAdapter.ConnectionViewHolder> {

    public interface OnAdapterLoadedListener {
        void onAdapterLoaded(boolean isEmpty);
    }

    private final PublicizeConnectionList mConnections = new PublicizeConnectionList();

    private final long mSiteId;
    private final long mCurrentUserId;
    private final String mServiceId;

    private PublicizeActions.OnPublicizeActionListener mActionListener;
    private OnAdapterLoadedListener mLoadedListener;

    public PublicizeConnectionAdapter(Context context, long siteId, String serviceId, long currentUserId) {
        super();
        mSiteId = siteId;
        mServiceId = StringUtils.notNullStr(serviceId);
        mCurrentUserId = currentUserId;
        setHasStableIds(true);
    }

    public void setOnAdapterLoadedListener(OnAdapterLoadedListener listener) {
        mLoadedListener = listener;
    }

    public void setOnPublicizeActionListener(PublicizeActions.OnPublicizeActionListener listener) {
        mActionListener = listener;
    }

    public void refresh() {
        PublicizeConnectionList siteConnections = PublicizeTable.getConnectionsForSite(mSiteId);
        PublicizeConnectionList serviceConnections =
                siteConnections.getServiceConnectionsForUser(mCurrentUserId, mServiceId);

        if (!mConnections.isSameAs(serviceConnections)) {
            mConnections.clear();
            mConnections.addAll(serviceConnections);
            notifyDataSetChanged();
        }

        if (mLoadedListener != null) {
            mLoadedListener.onAdapterLoaded(isEmpty());
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
        final PublicizeConnection connection = mConnections.get(position);

        holder.txtUser.setText(connection.getExternalDisplayName());
        holder.divider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);

        if (connection.hasExternalProfilePictureUrl()) {
            holder.imgAvatar.setImageUrl(connection.getExternalProfilePictureUrl(),
                    WPNetworkImageView.ImageType.AVATAR);
        } else {
            holder.imgAvatar.showDefaultGravatarImage();
        }

        holder.btnConnect.setAction(PublicizeConstants.ConnectAction.DISCONNECT);
        holder.btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionListener != null) {
                    mActionListener.onRequestDisconnect(connection);
                }
            }
        });
    }

    class ConnectionViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtUser;
        private final ConnectButton btnConnect;
        private final WPNetworkImageView imgAvatar;
        private final View divider;

        public ConnectionViewHolder(View view) {
            super(view);
            txtUser = (TextView) view.findViewById(R.id.text_user);
            imgAvatar = (WPNetworkImageView) view.findViewById(R.id.image_avatar);
            btnConnect = (ConnectButton) view.findViewById(R.id.button_connect);
            divider = view.findViewById(R.id.divider);
        }
    }

}
