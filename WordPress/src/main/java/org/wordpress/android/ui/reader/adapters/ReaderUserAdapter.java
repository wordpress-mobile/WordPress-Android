package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderInterfaces.DataLoadedListener;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * owner must call setUsers() with the list of
 * users to display
 */
public class ReaderUserAdapter extends RecyclerView.Adapter<ReaderUserAdapter.UserViewHolder> {
    private final ReaderUserList mUsers = new ReaderUserList();
    private DataLoadedListener mDataLoadedListener;
    private final int mAvatarSz;

    public ReaderUserAdapter(Context context) {
        super();
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
        setHasStableIds(true);
    }

    public void setDataLoadedListener(DataLoadedListener listener) {
        mDataLoadedListener = listener;
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    private boolean isEmpty() {
        return (getItemCount() == 0);
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reader_listitem_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        final ReaderUser user = mUsers.get(position);

        holder.mTxtName.setText(user.getDisplayName());
        if (user.hasUrl()) {
            holder.mTxtUrl.setVisibility(View.VISIBLE);
            holder.mTxtUrl.setText(user.getUrlDomain());
            if (user.hasBlogId()) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ReaderActivityLauncher.showReaderBlogPreview(v.getContext(), user.blogId);
                    }
                });
                holder.mRootView.setEnabled(true);
            } else {
                holder.itemView.setOnClickListener(null);
                holder.mRootView.setEnabled(false);
            }
        } else {
            holder.mRootView.setEnabled(false);
            holder.mTxtUrl.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
        }

        if (user.hasAvatarUrl()) {
            holder.mImgAvatar.setImageUrl(
                    GravatarUtils.fixGravatarUrl(user.getAvatarUrl(), mAvatarSz),
                    WPNetworkImageView.ImageType.AVATAR);
        } else {
            holder.mImgAvatar.showDefaultGravatarImageAndNullifyUrl();
        }
    }

    @Override
    public long getItemId(int position) {
        return mUsers.get(position).userId;
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTxtName;
        private final TextView mTxtUrl;
        private final WPNetworkImageView mImgAvatar;
        private final View mRootView;

        UserViewHolder(View view) {
            super(view);
            mRootView = view;
            mTxtName = (TextView) view.findViewById(R.id.text_name);
            mTxtUrl = (TextView) view.findViewById(R.id.text_url);
            mImgAvatar = (WPNetworkImageView) view.findViewById(R.id.image_avatar);
        }
    }

    public void setUsers(final ReaderUserList users) {
        mUsers.clear();
        if (users != null && users.size() > 0) {
            mUsers.addAll(users);
        }
        notifyDataSetChanged();
        if (mDataLoadedListener != null) {
            mDataLoadedListener.onDataLoaded(isEmpty());
        }
    }
}
