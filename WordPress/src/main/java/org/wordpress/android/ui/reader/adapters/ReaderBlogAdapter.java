package org.wordpress.android.ui.reader.adapters;

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
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderBlogList;
import org.wordpress.android.models.ReaderRecommendBlogList;
import org.wordpress.android.models.ReaderRecommendedBlog;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderAnim;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;

/*
 * adapter which shows either recommended or followed blogs - used by ReaderBlogFragment
 */
public class ReaderBlogAdapter extends BaseAdapter {
    public enum ReaderBlogType {RECOMMENDED, FOLLOWED}

    public interface BlogFollowChangeListener {
        public void onFollowBlogChanged();
    }

    private final LayoutInflater mInflater;
    private final ReaderBlogType mBlogType;
    private final boolean mCanUseStableIds;
    private final BlogFollowChangeListener mFollowListener;
    private final WeakReference<Context> mWeakContext;

    private ReaderRecommendBlogList mRecommendedBlogs = new ReaderRecommendBlogList();
    private ReaderBlogList mFollowedBlogs = new ReaderBlogList();

    public ReaderBlogAdapter(Context context,
                             ReaderBlogType blogType,
                             BlogFollowChangeListener followListener) {
        super();
        mWeakContext = new WeakReference<Context>(context);
        mInflater = LayoutInflater.from(context);
        mBlogType = blogType;
        mFollowListener = followListener;

        // recommended blogs all have a unique blogId, but followed blogs may have multiple
        // blogs with a blogId of zero
        mCanUseStableIds = (getBlogType() == ReaderBlogType.RECOMMENDED);
    }

    private Context getContext() {
        return mWeakContext.get();
    }

    public void refresh() {
        if (mIsTaskRunning) {
            AppLog.w(T.READER, "load blogs task is already running");
        } else {
            new LoadBlogsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    private boolean isPositionValid(int position) {
        return (position >= 0 && position < getCount());
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
    public View getView(final int position, View convertView, final ViewGroup parent) {
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
                final ReaderBlog blogInfo = (ReaderBlog) getItem(position);
                blogId = blogInfo.blogId;
                blogUrl = blogInfo.getUrl();
                isFollowing = blogInfo.isFollowing;
                String domain = UrlUtils.getDomainFromUrl(blogUrl);
                if (blogInfo.hasName()) {
                    holder.txtTitle.setText(blogInfo.getName());
                } else {
                    holder.txtTitle.setText(domain);
                }
                holder.txtUrl.setText(domain);
                holder.imgBlog.setImageUrl(blogInfo.getImageUrl(), WPNetworkImageView.ImageType.AVATAR);
                break;

            default:
                blogId = 0;
                blogUrl = null;
                isFollowing = false;
                break;
        }

        // show the correct following status
        ReaderUtils.showFollowStatus(holder.txtFollow, isFollowing);
        holder.txtFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReaderAnim.animateFollowButton(holder.txtFollow);
                changeFollowStatus(holder.txtFollow, position, !isFollowing);
            }
        });

        // show blog preview when view is clicked
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // make sure we have either the blog id or url
                if (blogId != 0 || !TextUtils.isEmpty(blogUrl)) {
                    ReaderActivityLauncher.showReaderBlogPreview(getContext(), blogId, blogUrl);
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

            // followed blogs don't have a description
            switch (getBlogType()) {
                case FOLLOWED:
                    txtDescription.setVisibility(View.GONE);
                    break;
                case RECOMMENDED:
                    txtDescription.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    private void changeFollowStatus(final TextView txtFollow,
                                    final int position,
                                    final boolean isAskingToFollow) {
        if (!isPositionValid(position)) {
            return;
        }

        final long blogId;
        final String blogUrl;
        switch (getBlogType()) {
            case RECOMMENDED:
                ReaderRecommendedBlog blog = mRecommendedBlogs.get(position);
                blogId = blog.blogId;
                blogUrl = blog.getBlogUrl();
                break;
            case FOLLOWED:
                ReaderBlog info = mFollowedBlogs.get(position);
                blogId = info.blogId;
                blogUrl = info.getUrl();
                break;
            default:
                return;
        }

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!succeeded && getContext() != null) {
                    int resId = (isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog);
                    ToastUtils.showToast(getContext(), resId);
                    ReaderUtils.showFollowStatus(txtFollow, !isAskingToFollow);
                    checkFollowStatus();
                }
            }
        };
        if (ReaderBlogActions.performFollowAction(blogId, blogUrl, isAskingToFollow, actionListener)) {
            if (getBlogType() == ReaderBlogType.FOLLOWED) {
                mFollowedBlogs.get(position).isFollowing = isAskingToFollow;
            }
            ReaderUtils.showFollowStatus(txtFollow, isAskingToFollow);
            notifyDataSetChanged(); // <-- required for getView() to know correct follow status
            if (mFollowListener != null) {
                mFollowListener.onFollowBlogChanged();
            }
        }
    }

    private boolean mIsTaskRunning = false;
    private class LoadBlogsTask extends AsyncTask<Void, Void, Boolean> {
        ReaderRecommendBlogList tmpRecommendedBlogs;
        ReaderBlogList tmpFollowedBlogs;

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
                    // get recommended blogs using this offset, then start over with no offset
                    // if there aren't any with this offset,
                    int limit = ReaderConstants.READER_MAX_RECOMMENDED_TO_DISPLAY;
                    int offset = AppPrefs.getReaderRecommendedBlogOffset();
                    tmpRecommendedBlogs = ReaderBlogTable.getRecommendedBlogs(limit, offset);
                    if (tmpRecommendedBlogs.size() == 0 && offset > 0) {
                        AppPrefs.setReaderRecommendedBlogOffset(0);
                        tmpRecommendedBlogs = ReaderBlogTable.getRecommendedBlogs(limit, 0);
                    }
                    return !mRecommendedBlogs.isSameList(tmpRecommendedBlogs);

                case FOLLOWED:
                    tmpFollowedBlogs = ReaderBlogTable.getFollowedBlogs();
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
                        mFollowedBlogs = (ReaderBlogList) (tmpFollowedBlogs.clone());
                        // sort followed blogs by name/domain to match display
                        Collections.sort(mFollowedBlogs, new Comparator<ReaderBlog>() {
                            @Override
                            public int compare(ReaderBlog thisBlog, ReaderBlog thatBlog) {
                                String thisName = getBlogNameForComparison(thisBlog);
                                String thatName = getBlogNameForComparison(thatBlog);
                                return thisName.compareToIgnoreCase(thatName);
                            }
                        });
                        break;
                }
                notifyDataSetChanged();
            }

            mIsTaskRunning = false;
        }

        private String getBlogNameForComparison(ReaderBlog blog) {
            if (blog == null) {
                return "";
            } else if (blog.hasName()) {
                return blog.getName();
            } else if (blog.hasUrl()) {
                return StringUtils.notNullStr(UrlUtils.getDomainFromUrl(blog.getUrl()));
            } else {
                return "";
            }
        }
    }
}
