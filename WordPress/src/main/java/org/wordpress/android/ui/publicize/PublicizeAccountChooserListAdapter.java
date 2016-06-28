package org.wordpress.android.ui.publicize;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;

public class PublicizeAccountChooserListAdapter extends RecyclerView.Adapter<PublicizeAccountChooserListAdapter.ViewHolder> {
    private List<PublicizeConnection> mConnectionItems;
    private OnPublicizeAccountChooserListener mListener;
    private boolean mAreAccountsConnected;
    private int mSelectedPosition;

    public PublicizeAccountChooserListAdapter(List<PublicizeConnection> connectionItems, OnPublicizeAccountChooserListener listener, boolean isConnected) {
        mConnectionItems = connectionItems;
        mListener = listener;
        mAreAccountsConnected = isConnected;
        mSelectedPosition = 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.publicize_connection_list_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final PublicizeConnection connection = mConnectionItems.get(position);
        holder.mProfileImageView.setImageUrl(connection.getExternalProfilePictureUrl(), WPNetworkImageView.ImageType.PHOTO);
        holder.mNameTextView.setText(connection.getExternalDisplayName());
        holder.mRadioButton.setChecked(position == mSelectedPosition);

        if (!mAreAccountsConnected) {
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mSelectedPosition = holder.getAdapterPosition();
                        mListener.onAccountSelected(mSelectedPosition);
                    }
                }
            });
        } else {
            holder.mRadioButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mConnectionItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final RadioButton mRadioButton;
        public final WPNetworkImageView mProfileImageView;
        public final TextView mNameTextView;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mRadioButton = (RadioButton) view.findViewById(R.id.radio_button);
            mProfileImageView = (WPNetworkImageView) view.findViewById(R.id.profile_pic);
            mNameTextView = (TextView) view.findViewById(R.id.name);
        }
    }

    public interface OnPublicizeAccountChooserListener {
        void onAccountSelected(int selectedIndex);
    }
}
