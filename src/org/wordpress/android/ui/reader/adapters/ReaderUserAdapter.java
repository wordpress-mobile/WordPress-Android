package org.wordpress.android.ui.reader.adapters;

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
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * Created by nbradbury on 6/27/13.
 */
public class ReaderUserAdapter extends BaseAdapter {
    public static enum ReaderUserListType {UNKNOWN, LIKE_POST}

    private final LayoutInflater mInflater;
    private ReaderUserList mUsers = new ReaderUserList();
    private ReaderUserListType mListType = ReaderUserListType.UNKNOWN;
    private final ReaderActions.DataLoadedListener mDataLoadedListener;
    private final ReaderPost mPost;
    private final int mAvatarSz;

    public ReaderUserAdapter(Context context,
                             ReaderUserListType listType,
                             ReaderPost post,
                             ReaderActions.DataLoadedListener dataLoadedListener) {
        super();
        mInflater = LayoutInflater.from(context);
        mListType = listType;
        mDataLoadedListener = dataLoadedListener;
        mPost = post;
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
        loadUsers();
    }

    @SuppressLint("NewApi")
    private void loadUsers() {
        if (mIsTaskRunning)
            AppLog.w(T.READER, "user task already running");

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
            convertView = mInflater.inflate(R.layout.reader_listitem_user, parent, false);
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
            holder.txtUrl.setText(user.getUrlDomain());
            // tapping anywhere in the view shows the user's blog
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.openUrl(v.getContext(), user.getUrl());
                }
            });

            // since the user has a blog url, enable following/unfollowing it
            if (holder.txtFollow.isSelected() != user.isFollowed)
                showFollowStatus(holder.txtFollow, user.isFollowed);
            holder.txtFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AniUtils.zoomAction(holder.txtFollow);
                    toggleFollowUser(user, holder.txtFollow);
                }
            });
            holder.txtFollow.setVisibility(View.VISIBLE);
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

        showFollowStatus(txtFollow, isAskingToFollow);
    }

    private void showFollowStatus(TextView txtFollow, boolean isFollowing) {
        txtFollow.setText(isFollowing ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
        int drawableId = (isFollowing ? R.drawable.note_icon_following : R.drawable.note_icon_follow);
        txtFollow.setCompoundDrawablesWithIntrinsicBounds(drawableId, 0, 0, 0);
        txtFollow.setSelected(isFollowing);
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

            // flag followed users, set avatar urls for use with photon, and pre-load user domains
            // so we can avoid having to do this for each user when getView() is called
            ReaderUrlList followedBlogUrls = ReaderBlogTable.getFollowedBlogUrls();
            for (ReaderUser user: tmpUsers) {
                user.isFollowed = user.hasUrl() && followedBlogUrls.contains(user.getUrl());
                user.setAvatarUrl(PhotonUtils.fixAvatar(user.getAvatarUrl(), mAvatarSz));
                user.getUrlDomain();
            }

            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mUsers = (ReaderUserList)(tmpUsers.clone());
                notifyDataSetChanged();
            }

            if (mDataLoadedListener!=null)
                mDataLoadedListener.onDataLoaded(isEmpty());

            mIsTaskRunning = false;
        }
    }

}
