package org.wordpress.android.ui.reader.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderRecommendBlogList;
import org.wordpress.android.models.ReaderRecommendedBlog;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions.BlogAction;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class ReaderBlogAdapter extends BaseAdapter {
    // TODO: implement followed blogs
    public enum ReaderBlogType { RECOMMENDED, FOLLOWED }

    private final LayoutInflater mInflater;
    private ReaderRecommendBlogList mRecommendedBlogs = new ReaderRecommendBlogList();
    private ReaderBlogType mBlogType;

    public ReaderBlogAdapter(Context context, ReaderBlogType blogType) {
        super();
        mInflater = LayoutInflater.from(context);
        mBlogType = blogType;
    }

    @SuppressLint("NewApi")
    public void refresh() {
        if (mIsTaskRunning) {
            AppLog.w(T.READER, "load blogs task is already running");
        }

        if (SysUtils.canUseExecuteOnExecutor()) {
            new LoadBlogsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new LoadBlogsTask().execute();
        }
    }

    public ReaderBlogType getBlogType() {
        return mBlogType;
    }

    @Override
    public int getCount() {
        switch (getBlogType()) {
            case RECOMMENDED:
                return mRecommendedBlogs.size();
            default:
                return 0;
        }

    }

    @Override
    public Object getItem(int position) {
        switch (getBlogType()) {
            case RECOMMENDED:
                return mRecommendedBlogs.get(position);
            default:
                return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        final BlogViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.reader_listitem_blog, parent, false);
            holder = new BlogViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (BlogViewHolder) convertView.getTag();
        }

        final boolean isFollowing;
        final long blogId;
        final String blogUrl;
        switch (getBlogType()) {
            case RECOMMENDED:
                final ReaderRecommendedBlog blog = (ReaderRecommendedBlog) getItem(position);
                blogId = blog.blogId;
                blogUrl = blog.getBlogUrl();
                isFollowing = ReaderBlogTable.isFollowedBlogUrl(blogUrl);
                holder.txtTitle.setText(blog.getTitle());
                holder.txtDescription.setText(blog.getReason());
                holder.txtUrl.setText(UrlUtils.getDomainFromUrl(blogUrl));
                holder.imgBlog.setImageUrl(blog.getImageUrl(), WPNetworkImageView.ImageType.AVATAR);
                break;
            default:
                isFollowing = false;
                blogId = 0;
                blogUrl = null;
                break;
        }

        // TODO: rewrite following once blog detail PR is merged
        showFollowStatus(holder.txtFollow, isFollowing);
        holder.txtFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeFollowStatus(holder.txtFollow, blogId, blogUrl, !isFollowing);
            }
        });

        // TODO: show blog detail rather than open url
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(blogUrl)) {
                    ReaderActivityLauncher.openUrl(parent.getContext(), blogUrl);
                }
            }
        });

        return convertView;
    }

    private static class BlogViewHolder {
        private final TextView txtTitle;
        private final TextView txtDescription;
        private final TextView txtUrl;
        private final TextView txtFollow;
        private final WPNetworkImageView imgBlog;

        BlogViewHolder(View view) {
            txtTitle = (TextView) view.findViewById(R.id.text_title);
            txtDescription = (TextView) view.findViewById(R.id.text_description);
            txtUrl = (TextView) view.findViewById(R.id.text_url);
            txtFollow = (TextView) view.findViewById(R.id.text_follow);
            imgBlog = (WPNetworkImageView) view.findViewById(R.id.image_blog);
        }
    }

    private void showFollowStatus(TextView txtFollow, boolean isFollowing) {
        txtFollow.setText(isFollowing ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
        int drawableId = (isFollowing ? R.drawable.note_icon_following : R.drawable.note_icon_follow);
        txtFollow.setCompoundDrawablesWithIntrinsicBounds(drawableId, 0, 0, 0);
        txtFollow.setSelected(isFollowing);
    }

    private void changeFollowStatus(TextView txtFollow, long blogId, String blogUrl, boolean isAskingToFollow) {
        BlogAction action = (isAskingToFollow ? BlogAction.FOLLOW : BlogAction.UNFOLLOW);
        if (ReaderBlogActions.performBlogAction(action, blogUrl)) {
            showFollowStatus(txtFollow, isAskingToFollow);
        }
    }

    private boolean mIsTaskRunning = false;
    private class LoadBlogsTask extends AsyncTask<Void, Void, Boolean> {
        ReaderRecommendBlogList tmpRecommendedBlogs;
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
            switch (getBlogType()) {
                case RECOMMENDED:
                    tmpRecommendedBlogs = ReaderBlogTable.getRecommendedBlogs();
                    return (tmpRecommendedBlogs != null && tmpRecommendedBlogs.size() > 0);
                default:
                    return false;
            }

        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                switch (getBlogType()) {
                    case RECOMMENDED:
                        mRecommendedBlogs = (ReaderRecommendBlogList) (tmpRecommendedBlogs.clone());
                        notifyDataSetChanged();
                        break;
                }
            }
            mIsTaskRunning = false;
        }
    }


}
