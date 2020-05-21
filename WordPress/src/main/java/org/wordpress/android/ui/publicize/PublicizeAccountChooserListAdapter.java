package org.wordpress.android.ui.publicize;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.List;

import javax.inject.Inject;

public class PublicizeAccountChooserListAdapter
        extends RecyclerView.Adapter<PublicizeAccountChooserListAdapter.ViewHolder> {
    private List<PublicizeConnection> mConnectionItems;
    private OnPublicizeAccountChooserListener mListener;
    private boolean mAreAccountsConnected;
    private int mSelectedPosition;

    @Inject ImageManager mImageManager;

    public PublicizeAccountChooserListAdapter(Context context, List<PublicizeConnection> connectionItems,
                                              OnPublicizeAccountChooserListener listener, boolean isConnected) {
        ((WordPress) context.getApplicationContext()).component().inject(this);
        mConnectionItems = connectionItems;
        mListener = listener;
        mAreAccountsConnected = isConnected;
        mSelectedPosition = 0;
    }

    @Override
    public @NotNull ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.publicize_connection_list_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final PublicizeConnection connection = mConnectionItems.get(position);
        mImageManager.load(holder.mProfileImageView, ImageType.PHOTO, connection.getExternalProfilePictureUrl());
        holder.mNameTextView.setText(getName(connection));
        holder.mRadioButton.setChecked(position == mSelectedPosition);

        if (!mAreAccountsConnected) {
            holder.mView.setOnClickListener(view -> {
                if (mListener != null) {
                    mSelectedPosition = holder.getAdapterPosition();
                    mListener.onAccountSelected(mSelectedPosition);
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
        final View mView;
        final RadioButton mRadioButton;
        final ImageView mProfileImageView;
        final TextView mNameTextView;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mRadioButton = view.findViewById(R.id.radio_button);
            mProfileImageView = view.findViewById(R.id.profile_pic);
            mNameTextView = view.findViewById(R.id.name);
        }
    }

    public interface OnPublicizeAccountChooserListener {
        void onAccountSelected(int selectedIndex);
    }

    private String getName(PublicizeConnection connection) {
        String name = connection.getExternalName();

        if (name.isEmpty()) {
            name = connection.getExternalDisplayName();
        }

        return name;
    }
}
