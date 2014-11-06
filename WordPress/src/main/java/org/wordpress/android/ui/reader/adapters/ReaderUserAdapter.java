package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
public class ReaderUserAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private ReaderUserList mUsers = new ReaderUserList();
    private final DataLoadedListener mDataLoadedListener;
    private final int mAvatarSz;

    public ReaderUserAdapter(Context context, DataLoadedListener dataLoadedListener) {
        super();
        mInflater = LayoutInflater.from(context);
        mDataLoadedListener = dataLoadedListener;
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return mUsers.size();
    }

    @Override
    public Object getItem(int position) {
        return mUsers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mUsers.get(position).userId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ReaderUser user = mUsers.get(position);
        final UserViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.reader_listitem_user, parent, false);
            holder = new UserViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (UserViewHolder) convertView.getTag();
        }

        holder.txtName.setText(user.getDisplayName());
        if (user.hasUrl()) {
            holder.txtUrl.setVisibility(View.VISIBLE);
            holder.txtUrl.setText(user.getUrlDomain());
            convertView.setEnabled(true);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (user.hasBlogId()) {
                        ReaderActivityLauncher.showReaderBlogPreview(v.getContext(), user.blogId, user.getUrl());
                    }
                }
            });
        } else {
            holder.txtUrl.setVisibility(View.GONE);
            convertView.setOnClickListener(null);
            convertView.setEnabled(false);
        }

        holder.imgAvatar.setImageUrl(user.getAvatarUrl(), WPNetworkImageView.ImageType.AVATAR);

        return convertView;
    }

    private static class UserViewHolder {
        private final TextView txtName;
        private final TextView txtUrl;
        private final WPNetworkImageView imgAvatar;

        UserViewHolder(View view) {
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
