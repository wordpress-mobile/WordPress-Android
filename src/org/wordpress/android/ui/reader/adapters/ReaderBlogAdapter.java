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
import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.models.ReaderBlogInfoList;
import org.wordpress.android.models.ReaderRecommendBlogList;
import org.wordpress.android.models.ReaderRecommendedBlog;
import org.wordpress.android.ui.prefs.UserPrefs;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/*
 * adapter which shows either recommended or followed blogs - used by ReaderBlogFragment
 */
public class ReaderBlogAdapter extends BaseAdapter {
    public enum ReaderBlogType {RECOMMENDED, FOLLOWED}

    public interface BlogFollowChangeListener {
        public void onFollowBlogChanged(long blogId, String blogUrl, boolean isFollowed);
    }

    private final LayoutInflater mInflater;
    private final ReaderBlogType mBlogType;
    private final boolean mCanUseStableIds;
    private final BlogFollowChangeListener mFollowListener;

    private ReaderRecommendBlogList mRecommendedBlogs = new ReaderRecommendBlogList();
    private ReaderBlogInfoList mFollowedBlogs = new ReaderBlogInfoList();

    public ReaderBlogAdapter(Context context,
                             ReaderBlogType blogType,
                             BlogFollowChangeListener followListener) {
        super();
        mInflater = LayoutInflater.from(context);
        mBlogType = blogType;
        mFollowListener = followListener;

        // recommended blogs all have a unique blogId, but followed blogs may have multiple
        // blogs with a blogId of zero
        mCanUseStableIds = (getBlogType() == ReaderBlogType.RECOMMENDED);
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

    /*
     * make sure the follow status of all blogs is accurate
     */
    public void checkFollowStatus() {
        switch (getBlogType()) {
            case FOLLOWED:
                // followed blogs store their follow status in the local db, so refreshing from
                // the local db will ensure the correct follow status is shown
                refresh();
                break;
            case RECOMMENDED:
                // recommended blogs check their follow status in getView(), so notifyDataSetChanged()
                // will ensure the correct follow status is shown
                notifyDataSetChanged();
                break;
        }
    }

    private ReaderBlogType getBlogType() {
        return mBlogType;
    }

    @Override
    public int getCount() {
        switch (getBlogType()) {
            case RECOMMENDED:
                return mRecommendedBlogs.size();
            case FOLLOWED:
                return mFollowedBlogs.size();
            default:
                return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        switch (getBlogType()) {
            case RECOMMENDED:
                return mRecommendedBlogs.get(position);
            case FOLLOWED:
                return mFollowedBlogs.get(position);
            default:
                return null;
        }
    }

    @Override
    public boolean hasStableIds() {
        return mCanUseStableIds;
    }

    @Override
    public long getItemId(int position) {
        if (mCanUseStableIds && getBlogType() == ReaderBlogType.RECOMMENDED) {
            return mRecommendedBlogs.get(position).blogId;
        } else {
            return position;
        }
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        final BlogViewHolder holder;
        if (convertView == null || !(convertView.getTag() instanceof BlogViewHolder)) {
            convertView = mInflater.inflate(R.layout.reader_listitem_blog, parent, false);
            holder = new BlogViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (BlogViewHolder) convertView.getTag();
        }

        final long blogId;
        final String blogUrl;
        final boolean isFollowing;
        switch (getBlogType()) {
            case RECOMMENDED:
                final ReaderRecommendedBlog blog = (ReaderRecommendedBlog) getItem(position);
                blogId = blog.blogId;
                blogUrl = blog.getBlogUrl();
                isFollowing = ReaderBlogTable.isFollowedBlog(blogId, blogUrl);
                holder.txtTitle.setText(blog.getTitle());
                holder.txtDescription.setText(blog.getReason());
                holder.txtUrl.setText(UrlUtils.getDomainFromUrl(blogUrl));
                holder.imgBlog.setImageUrl(blog.getImageUrl(), WPNetworkImageView.ImageType.AVATAR);
                break;

            case FOLLOWED:
                final ReaderBlogInfo blogInfo = (ReaderBlogInfo) getItem(position);
                blogId = blogInfo.blogId;
                blogUrl = blogInfo.getUrl();
                isFollowing = blogInfo.isFollowing;
                if (blogInfo.hasName()) {
                    holder.txtTitle.setText(blogInfo.getName());
                    holder.txtTitle.setVisibility(View.VISIBLE);
                } else {
                    holder.txtTitle.setVisibility(View.GONE);
                }
                holder.txtUrl.setText(UrlUtils.getDomainFromUrl(blogUrl));
                break;

            default:
                blogId = 0;
                blogUrl = null;
                isFollowing = false;
                break;
        }

        // show the correct following status
        showFollowStatus(holder.txtFollow, isFollowing);
        holder.txtFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AniUtils.zoomAction(holder.txtFollow);
                changeFollowStatus(holder.txtFollow, blogId, blogUrl, !isFollowing);
            }
        });

        // show blog detail when view is clicked
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // make sure we have either the blog id or url
                if (blogId != 0 || !TextUtils.isEmpty(blogUrl)) {
                    ReaderActivityLauncher.showReaderBlogDetail(parent.getContext(), blogId, blogUrl);
                }
            }
        });

        return convertView;
    }

    private class BlogViewHolder {
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

            switch (getBlogType()) {
                case FOLLOWED:
                    txtDescription.setVisibility(View.GONE);
                    imgBlog.setVisibility(View.GONE);
                    break;
                case RECOMMENDED:
                    txtDescription.setVisibility(View.VISIBLE);
                    imgBlog.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    private void showFollowStatus(TextView txtFollow, boolean isFollowing) {
        txtFollow.setText(isFollowing ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
        int drawableId = (isFollowing ? R.drawable.note_icon_following : R.drawable.note_icon_follow);
        txtFollow.setCompoundDrawablesWithIntrinsicBounds(drawableId, 0, 0, 0);
        txtFollow.setSelected(isFollowing);
    }

    private void changeFollowStatus(TextView txtFollow, long blogId, String blogUrl, boolean isAskingToFollow) {
        if (ReaderBlogActions.performFollowAction(blogId, blogUrl, isAskingToFollow, null)) {
            showFollowStatus(txtFollow, isAskingToFollow);
            notifyDataSetChanged(); // <-- required for getView() to know correct follow status
            if (mFollowListener != null) {
                mFollowListener.onFollowBlogChanged(blogId, blogUrl, isAskingToFollow);
            }
        }
    }

    private boolean mIsTaskRunning = false;
    private class LoadBlogsTask extends AsyncTask<Void, Void, Boolean> {
        ReaderRecommendBlogList tmpRecommendedBlogs;
        ReaderBlogInfoList tmpFollowedBlogs;

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
                    int limit = ReaderConstants.READER_MAX_RECOMMENDED_TO_DISPLAY;
                    int offset = UserPrefs.getReaderRecommendedBlogOffset();
                    tmpRecommendedBlogs = ReaderBlogTable.getRecommendedBlogs(limit, offset);
                    // if there aren't any with this offset, start over with no offset
                    if (tmpRecommendedBlogs.size() == 0 && offset > 0) {
                        UserPrefs.setReaderRecommendedBlogOffset(0);
                        tmpRecommendedBlogs = ReaderBlogTable.getRecommendedBlogs(limit, 0);
                    }
                    return !mRecommendedBlogs.isSameList(tmpRecommendedBlogs);
                case FOLLOWED:
                    tmpFollowedBlogs = ReaderBlogTable.getAllFollowedBlogInfo();
                    return !mFollowedBlogs.isSameList(tmpFollowedBlogs);
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
                        break;
                    case FOLLOWED:
                        mFollowedBlogs = (ReaderBlogInfoList) (tmpFollowedBlogs.clone());
                        break;
                }
                notifyDataSetChanged();
            }

            mIsTaskRunning = false;
        }
    }
}
