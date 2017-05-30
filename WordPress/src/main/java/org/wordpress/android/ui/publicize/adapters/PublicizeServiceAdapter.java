package org.wordpress.android.ui.publicize.adapters;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.models.PublicizeConnectionList;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.models.PublicizeServiceList;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.Collections;
import java.util.Comparator;

public class PublicizeServiceAdapter extends RecyclerView.Adapter<PublicizeServiceAdapter.SharingViewHolder> {

    public interface OnAdapterLoadedListener {
        void onAdapterLoaded(boolean isEmpty);
    }
    public interface OnServiceClickListener {
        void onServiceClicked(PublicizeService service);
    }

    private final PublicizeServiceList mServices = new PublicizeServiceList();
    private final PublicizeConnectionList mConnections = new PublicizeConnectionList();

    private final long mSiteId;
    private final int mBlavatarSz;
    private final ColorFilter mGrayScaleFilter;
    private final long mCurrentUserId;

    private OnAdapterLoadedListener mAdapterLoadedListener;
    private OnServiceClickListener mServiceClickListener;

    public PublicizeServiceAdapter(Context context, long siteId, long currentUserId) {
        super();

        mSiteId = siteId;
        mBlavatarSz = context.getResources().getDimensionPixelSize(R.dimen.blavatar_sz_small);
        mCurrentUserId = currentUserId;

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        mGrayScaleFilter = new ColorMatrixColorFilter(matrix);

        setHasStableIds(true);
    }

    public void setOnAdapterLoadedListener(OnAdapterLoadedListener listener) {
        mAdapterLoadedListener = listener;
    }

    public void setOnServiceClickListener(OnServiceClickListener listener) {
        mServiceClickListener = listener;
    }

    public void refresh() {
        if (!mIsTaskRunning) {
            new LoadServicesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public void reload() {
        clear();
        refresh();
    }

    private void clear() {
        mServices.clear();
        mConnections.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mServices.size();
    }

    private boolean isEmpty() {
        return (getItemCount() == 0);
    }

    @Override
    public long getItemId(int position) {
        return mServices.get(position).getId().hashCode();
    }

    @Override
    public SharingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.publicize_listitem_service, parent, false);
        return new SharingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SharingViewHolder holder, int position) {
        final PublicizeService service = mServices.get(position);
        final PublicizeConnectionList connections = mConnections.getServiceConnectionsForUser(mCurrentUserId, service.getId());

        holder.txtService.setText(service.getLabel());
        String iconUrl = PhotonUtils.getPhotonImageUrl(service.getIconUrl(), mBlavatarSz, mBlavatarSz);
        holder.imgIcon.setImageUrl(iconUrl, WPNetworkImageView.ImageType.BLAVATAR);

        if (connections.size() > 0) {
            holder.txtUser.setText(connections.getUserDisplayNames());
            holder.txtUser.setVisibility(View.VISIBLE);
            holder.imgIcon.clearColorFilter();
            holder.imgIcon.setImageAlpha(255);
        } else {
            holder.txtUser.setVisibility(View.GONE);
            holder.imgIcon.setColorFilter(mGrayScaleFilter);
            holder.imgIcon.setImageAlpha(128);
        }

        // show divider for all but the first item
        holder.divider.setVisibility(position > 0 ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mServiceClickListener != null) {
                    mServiceClickListener.onServiceClicked(service);
                }
            }
        });
    }

    class SharingViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtService;
        private final TextView txtUser;
        private final View divider;
        private final WPNetworkImageView imgIcon;

        public SharingViewHolder(View view) {
            super(view);
            txtService = (TextView) view.findViewById(R.id.text_service);
            txtUser = (TextView) view.findViewById(R.id.text_user);
            imgIcon = (WPNetworkImageView) view.findViewById(R.id.image_icon);
            divider = view.findViewById(R.id.divider);
        }
    }

    /*
     * AsyncTask to load services
     */
    private boolean mIsTaskRunning = false;
    private class LoadServicesTask extends AsyncTask<Void, Void, Boolean> {
        private PublicizeServiceList tmpServices;
        private PublicizeConnectionList tmpConnections;

        @Override
        protected void onPreExecute() {
            mIsTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsTaskRunning = false;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            tmpServices = PublicizeTable.getServiceList();
            tmpConnections = PublicizeTable.getConnectionsForSite(mSiteId);
            return !(tmpServices.isSameAs(mServices) && tmpConnections.isSameAs(mConnections));
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mServices.clear();
                mServices.addAll(tmpServices);

                mConnections.clear();
                mConnections.addAll(tmpConnections);
                sortConnections();

                notifyDataSetChanged();
            }

            mIsTaskRunning = false;

            if (mAdapterLoadedListener != null) {
                mAdapterLoadedListener.onAdapterLoaded(isEmpty());
            }
        }

        /*
         * sort connected services to the top
         */
        private void sortConnections() {
            Collections.sort(mServices, new Comparator<PublicizeService>() {
                @Override
                public int compare(PublicizeService lhs, PublicizeService rhs) {
                    boolean isLhsConnected = mConnections.isServiceConnectedForUser(mCurrentUserId, lhs);
                    boolean isRhsConnected = mConnections.isServiceConnectedForUser(mCurrentUserId, rhs);
                    if (isLhsConnected && !isRhsConnected) {
                        return -1;
                    } else if (isRhsConnected && !isLhsConnected) {
                        return 1;
                    } else {
                        return lhs.getLabel().compareToIgnoreCase(rhs.getLabel());
                    }
                }
            });
        }
    }

}
