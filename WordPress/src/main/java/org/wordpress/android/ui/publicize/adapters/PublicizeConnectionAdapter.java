package org.wordpress.android.ui.publicize.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.models.PublicizeConnection.ConnectStatus;
import org.wordpress.android.models.PublicizeConnectionList;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.ui.publicize.ConnectButton;
import org.wordpress.android.ui.publicize.PublicizeActions;
import org.wordpress.android.ui.publicize.PublicizeConstants;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import javax.inject.Inject;

public class PublicizeConnectionAdapter extends RecyclerView.Adapter<PublicizeConnectionAdapter.ConnectionViewHolder> {
    public interface OnAdapterLoadedListener {
        void onAdapterLoaded(boolean isEmpty);
    }

    private final PublicizeConnectionList mConnections = new PublicizeConnectionList();

    private final long mSiteId;
    private final long mCurrentUserId;
    private final PublicizeService mService;

    private PublicizeActions.OnPublicizeActionListener mActionListener;
    private OnAdapterLoadedListener mLoadedListener;

    @Inject ImageManager mImageManager;

    public PublicizeConnectionAdapter(Context context, long siteId, PublicizeService service, long currentUserId) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);
        mSiteId = siteId;
        mService = service;
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
                siteConnections.getServiceConnectionsForUser(mCurrentUserId, mService.getId());

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

    @NotNull
    @Override
    public ConnectionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.publicize_listitem_connection, parent, false);
        return new ConnectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ConnectionViewHolder holder, int position) {
        final PublicizeConnection connection = mConnections.get(position);

        holder.mTxtUser.setText(connection.getExternalDisplayName());
        holder.mDivider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);

        mImageManager.loadIntoCircle(holder.mImgAvatar, ImageType.AVATAR_WITH_BACKGROUND,
                connection.getExternalProfilePictureUrl());

        bindButton(holder.mBtnConnect, connection);
    }

    private void bindButton(ConnectButton btnConnect, final PublicizeConnection connection) {
        ConnectStatus status = connection.getStatusEnum();
        switch (status) {
            case OK:
            case MUST_DISCONNECT:
                btnConnect.setAction(PublicizeConstants.ConnectAction.DISCONNECT);
                btnConnect.setOnClickListener(v -> {
                    if (mActionListener != null) {
                        mActionListener.onRequestDisconnect(connection);
                    }
                });
                break;
            case BROKEN:
            default:
                btnConnect.setAction(ConnectAction.RECONNECT);
                btnConnect.setOnClickListener(view -> {
                    if (mActionListener != null) {
                        mActionListener.onRequestReconnect(mService, connection);
                    }
                });
        }
    }

    class ConnectionViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTxtUser;
        private final ConnectButton mBtnConnect;
        private final ImageView mImgAvatar;
        private final View mDivider;

        ConnectionViewHolder(View view) {
            super(view);
            mTxtUser = view.findViewById(R.id.text_user);
            mImgAvatar = view.findViewById(R.id.image_avatar);
            mBtnConnect = view.findViewById(R.id.button_connect);
            mDivider = view.findViewById(R.id.divider);
        }
    }
}
