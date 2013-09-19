package org.wordpress.android.ui.reader_native.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.reader_native.ReaderActivityLauncher;
import org.wordpress.android.ui.reader_native.actions.ReaderBlogActions;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ReaderAniUtils;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * Created by nbradbury on 6/27/13.
 */
public class ReaderUserAdapter extends BaseAdapter {
    public static enum ReaderUserListType {UNKNOWN, LIKE_POST}

    private LayoutInflater mInflater;
    private ReaderUserList mUsers = new ReaderUserList();
    private ReaderUserListType mListType = ReaderUserListType.UNKNOWN;
    private ReaderPost mPost;
    private int mAvatarSz;

    public ReaderUserAdapter(Context context, ReaderUserListType listType, ReaderPost post) {
        super();
        mInflater = LayoutInflater.from(context);
        mListType = listType;
        mPost = post;
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.reader_avatar_sz_small);
        loadUsers();
    }

    @SuppressLint("NewApi")
    private void loadUsers() {
        if (mIsTaskRunning)
            ReaderLog.w("user task already running");

        if (SysUtils.canUseExecuteOnExecutor()) {
            new LoadUsersTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new LoadUsersTask().execute();
        }
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

        if (convertView==null) {
            convertView = mInflater.inflate(R.layout.listitem_reader_user, parent, false);
            holder = new UserViewHolder();
            holder.txtName = (TextView) convertView.findViewById(R.id.text_name);
            holder.txtUrl = (TextView) convertView.findViewById(R.id.text_url);
            holder.txtFollow = (TextView) convertView.findViewById(R.id.text_follow);
            holder.imgAvatar = (WPNetworkImageView) convertView.findViewById(R.id.image_avatar);
            convertView.setTag(holder);
        } else {
            holder = (UserViewHolder) convertView.getTag();
        }

        holder.txtName.setText(user.getDisplayName());
        if (user.hasUrl()) {
            holder.txtUrl.setVisibility(View.VISIBLE);
            holder.txtUrl.setText(UrlUtils.getDomainFromUrl(user.getUrl()));
            // tapping anywhere in the view shows the user's blog
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.openUrl(v.getContext(), user.getUrl());
                }
            });

            // since the user has a blog url, enable following/unfollowing it
            holder.txtFollow.setVisibility(View.VISIBLE);
            if (holder.txtFollow.isSelected()!=user.isFollowed) {
                holder.txtFollow.setSelected(user.isFollowed);
                holder.txtFollow.setText(user.isFollowed ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
            }
            holder.txtFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderAniUtils.zoomAction(holder.txtFollow);
                    toggleFollowUser(user, holder.txtFollow);
                }
            });
        } else {
            // no blog url, so can't follow
            holder.txtUrl.setVisibility(View.GONE);
            holder.txtFollow.setVisibility(View.GONE);
            convertView.setOnClickListener(null);
        }

        holder.imgAvatar.setImageUrl(user.getAvatarUrl(), WPNetworkImageView.ImageType.AVATAR);

        return convertView;
    }

    private void toggleFollowUser(ReaderUser user, TextView txtFollow) {
        if (user==null || !user.hasUrl())
            return;

        boolean isAskingToFollow = !user.isFollowed;
        ReaderBlogActions.BlogAction action = (isAskingToFollow ? ReaderBlogActions.BlogAction.FOLLOW : ReaderBlogActions.BlogAction.UNFOLLOW);

        if (!ReaderBlogActions.performBlogAction(action, user.getUrl()))
            return;

        if (isAskingToFollow) {
            user.isFollowed = true;
        } else {
            user.isFollowed = false;
        }

        if (txtFollow!=null) {
            txtFollow.setText(isAskingToFollow ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
            txtFollow.setSelected(isAskingToFollow);
        }
    }

    private static class UserViewHolder {
        private TextView txtName;
        private TextView txtUrl;
        private TextView txtFollow;
        private WPNetworkImageView imgAvatar;
    }

    private boolean mIsTaskRunning = false;
    private class LoadUsersTask extends AsyncTask<Void, Void, Boolean> {
        ReaderUserList tmpUsers;
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
            switch (mListType) {
                case LIKE_POST:
                    tmpUsers = ReaderUserTable.getUsersWhoLikePost(mPost, Constants.READER_MAX_USERS_TO_DISPLAY);
                    break;
                default :
                    break;
            }

            if (tmpUsers==null)
                return false;

            // flag followed users & set avatar urls for use with photon - avoids having to do
            // this for each user when getView() is called
            ReaderUrlList followedBlogUrls = ReaderBlogTable.getFollowedBlogUrls();
            for (ReaderUser user: tmpUsers) {
                user.isFollowed = user.hasUrl() && followedBlogUrls.contains(user.getUrl());
                user.setAvatarUrl(PhotonUtils.fixAvatar(user.getAvatarUrl(), mAvatarSz));
            }

            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mUsers = (ReaderUserList)(tmpUsers.clone());
                notifyDataSetChanged();
            }

            mIsTaskRunning = false;
        }
    }

}
