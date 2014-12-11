package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderInterfaces.DataLoadedListener;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * owner must call setUsers() with the list of
 * users to display
 */
public class ReaderUserAdapter  extends RecyclerView.Adapter<ReaderUserAdapter.UserViewHolder> {
    private ReaderUserList mUsers = new ReaderUserList();
    private DataLoadedListener mDataLoadedListener;
    private final int mAvatarSz;

    public ReaderUserAdapter(Context context) {
        super();
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
    }

    public void setDataLoadedListener(DataLoadedListener listener) {
        mDataLoadedListener = listener;
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    boolean isEmpty() {
        return (getItemCount() == 0);
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reader_cardview_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        final ReaderUser user = mUsers.get(position);

        holder.txtName.setText(user.getDisplayName());
        if (user.hasUrl()) {
            holder.txtUrl.setVisibility(View.VISIBLE);
            holder.txtUrl.setText(user.getUrlDomain());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (user.hasBlogId()) {
                        ReaderActivityLauncher.showReaderBlogPreview(v.getContext(), user.blogId, user.getUrl());
                    }
                }
            });
        } else {
            holder.txtUrl.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
        }

        holder.imgAvatar.setImageUrl(user.getAvatarUrl(), WPNetworkImageView.ImageType.AVATAR);
    }

    @Override
    public long getItemId(int position) {
        return mUsers.get(position).userId;
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtName;
        private final TextView txtUrl;
        private final WPNetworkImageView imgAvatar;

        public UserViewHolder(View view) {
            super(view);
            txtName = (TextView) view.findViewById(R.id.text_name);
            txtUrl = (TextView) view.findViewById(R.id.text_url);
            imgAvatar = (WPNetworkImageView) view.findViewById(R.id.image_avatar);
        }
    }

    private void clear() {
        if (mUsers.size() > 0) {
            mUsers.clear();
            notifyDataSetChanged();
        }
    }

    public void setUsers(final ReaderUserList users) {
        if (users == null || users.size() == 0) {
            clear();
            return;
        }

        mUsers = (ReaderUserList) users.clone();
        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                // flag followed users, set avatar urls for use with photon, and pre-load
                // user domains so we can avoid having to do this for each user when getView()
                // is called
                ReaderUrlList followedBlogUrls = ReaderBlogTable.getFollowedBlogUrls();
                for (ReaderUser user: mUsers) {
                    user.isFollowed = user.hasUrl() && followedBlogUrls.contains(user.getUrl());
                    user.setAvatarUrl(PhotonUtils.fixAvatar(user.getAvatarUrl(), mAvatarSz));
                    user.getUrlDomain();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                        if (mDataLoadedListener != null) {
                            mDataLoadedListener.onDataLoaded(isEmpty());
                        }
                    }
                });
            }
        }.start();
    }
}
