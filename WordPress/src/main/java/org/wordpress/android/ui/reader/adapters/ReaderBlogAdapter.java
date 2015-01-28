package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderBlogList;
import org.wordpress.android.models.ReaderRecommendBlogList;
import org.wordpress.android.models.ReaderRecommendedBlog;
import org.wordpress.android.ui.reader.ReaderInterfaces;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.views.ReaderFollowButton;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.Collections;
import java.util.Comparator;

/*
 * adapter which shows either recommended or followed blogs - used by ReaderBlogFragment
 */
public class ReaderBlogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;

    public enum ReaderBlogType {RECOMMENDED, FOLLOWED}

    public interface BlogFollowChangeListener {
        public void onFollowBlogChanged();
    }
    public interface BlogClickListener {
        public void onBlogClicked(Object blog);
    }

    private final ReaderBlogType mBlogType;
    private BlogFollowChangeListener mFollowListener;
    private BlogClickListener mClickListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;

    private ReaderRecommendBlogList mRecommendedBlogs = new ReaderRecommendBlogList();
    private ReaderBlogList mFollowedBlogs = new ReaderBlogList();

    @SuppressWarnings("UnusedParameters")
    public ReaderBlogAdapter(Context context, ReaderBlogType blogType) {
        super();
        setHasStableIds(false);
        mBlogType = blogType;
    }

    public void setFollowChangeListener(BlogFollowChangeListener listener) {
        mFollowListener = listener;
    }

    public void setDataLoadedListener(ReaderInterfaces.DataLoadedListener listener) {
        mDataLoadedListener = listener;
    }

    public void setBlogClickListener(BlogClickListener listener) {
        mClickListener = listener;
    }

    public void refresh() {
        if (mIsTaskRunning) {
            AppLog.w(T.READER, "load blogs task is already running");
            return;
        }
        new LoadBlogsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    public boolean isEmpty() {
        return (getItemCount() == 0);
    }

    @Override
    public int getItemCount() {
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
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_ITEM:
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.reader_listitem_blog, parent, false);
                return new BlogViewHolder(itemView);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof BlogViewHolder) {
            final BlogViewHolder blogHolder = (BlogViewHolder) holder;
            final boolean isFollowing;
            switch (getBlogType()) {
                case RECOMMENDED:
                    final ReaderRecommendedBlog blog = mRecommendedBlogs.get(position);
                    isFollowing = ReaderBlogTable.isFollowedBlog(blog.blogId, blog.getBlogUrl());
                    blogHolder.txtTitle.setText(blog.getTitle());
                    blogHolder.txtDescription.setText(blog.getReason());
                    blogHolder.txtUrl.setText(UrlUtils.getDomainFromUrl(blog.getBlogUrl()));
                    blogHolder.imgBlog.setImageUrl(blog.getImageUrl(), WPNetworkImageView.ImageType.SITE_AVATAR);
                    break;

                case FOLLOWED:
                    final ReaderBlog blogInfo = mFollowedBlogs.get(position);
                    isFollowing = blogInfo.isFollowing;
                    String domain = UrlUtils.getDomainFromUrl(blogInfo.getUrl());
                    if (blogInfo.hasName()) {
                        blogHolder.txtTitle.setText(blogInfo.getName());
                    } else {
                        blogHolder.txtTitle.setText(domain);
                    }
                    blogHolder.txtUrl.setText(domain);
                    blogHolder.imgBlog.setImageUrl(blogInfo.getImageUrl(), WPNetworkImageView.ImageType.SITE_AVATAR);
                    break;

                default:
                    isFollowing = false;
                    break;
            }

            blogHolder.followButton.setIsFollowed(isFollowing, false);
            blogHolder.followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changeFollowStatus((ReaderFollowButton) view, position, !isFollowing);
                }
            });

            if (mClickListener != null) {
                blogHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switch (getBlogType()) {
                            case RECOMMENDED:
                                mClickListener.onBlogClicked(mRecommendedBlogs.get(position));
                                break;
                            case FOLLOWED:
                                mClickListener.onBlogClicked(mFollowedBlogs.get(position));
                                break;
                        }
                    }
                });
            }
        }
    }

    /*
     * holder used for followed/recommended blogs
     */
    class BlogViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final TextView txtDescription;
        private final TextView txtUrl;
        private final ReaderFollowButton followButton;
        private final WPNetworkImageView imgBlog;

        public BlogViewHolder(View view) {
            super(view);

            txtTitle = (TextView) view.findViewById(R.id.text_title);
            txtDescription = (TextView) view.findViewById(R.id.text_description);
            txtUrl = (TextView) view.findViewById(R.id.text_url);
            followButton = (ReaderFollowButton) view.findViewById(R.id.follow_button);
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

    private void changeFollowStatus(final ReaderFollowButton followButton,
                                    final int position,
                                    final boolean isAskingToFollow) {
        if (getItemViewType(position) != VIEW_TYPE_ITEM) {
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
                Context context = followButton.getContext();
                if (!succeeded && context != null) {
                    int resId = (isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog);
                    ToastUtils.showToast(context, resId);
                    followButton.setIsFollowed(!isAskingToFollow, false);
                    checkFollowStatus();
                }
            }
        };

        followButton.setIsFollowed(isAskingToFollow, true);

        if (ReaderBlogActions.performFollowAction(blogId, blogUrl, isAskingToFollow, actionListener)) {
            if (getBlogType() == ReaderBlogType.FOLLOWED) {
                mFollowedBlogs.get(position).isFollowing = isAskingToFollow;
            }
            notifyItemChanged(position);
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
                    tmpRecommendedBlogs = ReaderBlogTable.getRecommendedBlogs();
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
                        mRecommendedBlogs = (ReaderRecommendBlogList) tmpRecommendedBlogs.clone();
                        break;
                    case FOLLOWED:
                        mFollowedBlogs = (ReaderBlogList) tmpFollowedBlogs.clone();
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

            if (mDataLoadedListener != null) {
                mDataLoadedListener.onDataLoaded(isEmpty());
            }
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
