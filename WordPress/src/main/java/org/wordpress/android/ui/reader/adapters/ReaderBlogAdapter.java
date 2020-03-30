package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderBlogList;
import org.wordpress.android.models.ReaderRecommendBlogList;
import org.wordpress.android.models.ReaderRecommendedBlog;
import org.wordpress.android.ui.reader.ReaderInterfaces;
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.views.ReaderFollowButton;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import javax.inject.Inject;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/*
 * adapter which shows either recommended or followed blogs - used by ReaderBlogFragment
 */
public class ReaderBlogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_ITEM = 0;

    public enum ReaderBlogType {
        RECOMMENDED, FOLLOWED
    }

    public interface BlogClickListener {
        void onBlogClicked(Object blog);
    }

    private final ReaderBlogType mBlogType;
    private BlogClickListener mClickListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;

    private ReaderRecommendBlogList mRecommendedBlogs = new ReaderRecommendBlogList();
    private ReaderBlogList mFollowedBlogs = new ReaderBlogList();

    private String mSearchFilter;

    @Inject protected ImageManager mImageManager;

    public ReaderBlogAdapter(Context context, ReaderBlogType blogType, String searchFilter) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);
        setHasStableIds(false);
        mBlogType = blogType;
        mSearchFilter = searchFilter;
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
                View itemView =
                        LayoutInflater.from(parent.getContext()).inflate(R.layout.reader_listitem_blog, parent, false);
                return new BlogViewHolder(itemView);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof BlogViewHolder) {
            final BlogViewHolder blogHolder = (BlogViewHolder) holder;
            switch (getBlogType()) {
                case RECOMMENDED:
                    final ReaderRecommendedBlog blog = mRecommendedBlogs.get(position);
                    blogHolder.mTxtTitle.setText(blog.getTitle());
                    blogHolder.mTxtDescription.setText(blog.getReason());
                    blogHolder.mTxtUrl.setText(UrlUtils.getHost(blog.getBlogUrl()));
                    mImageManager.load(blogHolder.mImgBlog, ImageType.BLAVATAR, blog.getImageUrl());
                    break;

                case FOLLOWED:
                    final ReaderBlog blogInfo = mFollowedBlogs.get(position);
                    if (blogInfo.hasName()) {
                        blogHolder.mTxtTitle.setText(blogInfo.getName());
                    } else {
                        blogHolder.mTxtTitle.setText(R.string.reader_untitled_post);
                    }
                    if (blogInfo.hasUrl()) {
                        blogHolder.mTxtUrl.setText(UrlUtils.getHost(blogInfo.getUrl()));
                    } else if (blogInfo.hasFeedUrl()) {
                        blogHolder.mTxtUrl.setText(UrlUtils.getHost(blogInfo.getFeedUrl()));
                    } else {
                        blogHolder.mTxtUrl.setText("");
                    }
                    mImageManager.load(blogHolder.mImgBlog, ImageType.BLAVATAR, blogInfo.getImageUrl());
                    blogHolder.mFollowButton.setIsFollowed(blogInfo.isFollowing);
                    blogHolder.mFollowButton.setOnClickListener(v -> toggleFollow(
                            blogHolder.itemView.getContext(),
                            blogHolder.mFollowButton,
                            blogInfo));
                    break;
            }

            if (mClickListener != null) {
                blogHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int clickedPosition = blogHolder.getAdapterPosition();
                        if (clickedPosition == RecyclerView.NO_POSITION) {
                            return;
                        }
                        switch (getBlogType()) {
                            case RECOMMENDED:
                                mClickListener.onBlogClicked(mRecommendedBlogs.get(clickedPosition));
                                break;
                            case FOLLOWED:
                                mClickListener.onBlogClicked(mFollowedBlogs.get(clickedPosition));
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
        private final TextView mTxtTitle;
        private final TextView mTxtDescription;
        private final TextView mTxtUrl;
        private final ImageView mImgBlog;
        private final ReaderFollowButton mFollowButton;

        BlogViewHolder(View view) {
            super(view);

            mTxtTitle = view.findViewById(R.id.text_title);
            mTxtDescription = view.findViewById(R.id.text_description);
            mTxtUrl = view.findViewById(R.id.text_url);
            mImgBlog = view.findViewById(R.id.image_blog);
            mFollowButton = view.findViewById(R.id.follow_button);

            // followed blogs don't have a description
            // recommended blogs don't have a follow button
            switch (getBlogType()) {
                case FOLLOWED:
                    mTxtDescription.setVisibility(GONE);
                    mFollowButton.setVisibility(VISIBLE);
                    break;
                case RECOMMENDED:
                    mTxtDescription.setVisibility(VISIBLE);
                    mFollowButton.setVisibility(GONE);
                    break;
            }
        }
    }

    private boolean mIsTaskRunning = false;

    private void toggleFollow(Context context, ReaderFollowButton followButton, ReaderBlog blog) {
        if (!NetworkUtils.checkConnection(context)) {
            return;
        }

        final boolean isAskingToFollow = !blog.isFollowing;

        // disable follow button until API call returns
        followButton.setEnabled(false);

        final ActionListener listener = succeeded -> {
            followButton.setEnabled(true);
            if (!succeeded) {
                int errResId = isAskingToFollow ? R.string.reader_toast_err_follow_blog
                        : R.string.reader_toast_err_unfollow_blog;
                ToastUtils.showToast(context, errResId);
                followButton.setIsFollowed(!isAskingToFollow);
                blog.isFollowing = !isAskingToFollow;
            }
        };

        final boolean result;

        if (blog.feedId != 0) {
            result = ReaderBlogActions.followFeedById(blog.feedId, isAskingToFollow, listener);
        } else {
            result = ReaderBlogActions.followBlogById(blog.blogId, isAskingToFollow, listener);
        }

        if (result) {
            followButton.setIsFollowedAnimated(isAskingToFollow);
            blog.isFollowing = isAskingToFollow;
        }
    }

    private class LoadBlogsTask extends AsyncTask<Void, Void, Boolean> {
        private ReaderRecommendBlogList mTmpRecommendedBlogs;
        private ReaderBlogList mTmpFollowedBlogs;

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
                    mTmpRecommendedBlogs = ReaderBlogTable.getRecommendedBlogs();
                    return !mRecommendedBlogs.isSameList(mTmpRecommendedBlogs);

                case FOLLOWED:
                    mTmpFollowedBlogs = new ReaderBlogList();
                    ReaderBlogList allFollowedBlogs = ReaderBlogTable.getFollowedBlogs();
                    if (hasSearchFilter()) {
                        String query = mSearchFilter.toLowerCase(Locale.getDefault());
                        for (ReaderBlog blog : allFollowedBlogs) {
                            if (blog.getName().toLowerCase(Locale.getDefault()).contains(query)) {
                                mTmpFollowedBlogs.add(blog);
                            } else if (UrlUtils.getHost(blog.getUrl()).toLowerCase(Locale.ROOT).contains(query)) {
                                mTmpFollowedBlogs.add(blog);
                            }
                        }
                    } else {
                        mTmpFollowedBlogs.addAll(allFollowedBlogs);
                    }
                    // sort followed blogs by name/domain to match display
                    Collections.sort(mTmpFollowedBlogs, new Comparator<ReaderBlog>() {
                        @Override
                        public int compare(ReaderBlog thisBlog, ReaderBlog thatBlog) {
                            String thisName = getBlogNameForComparison(thisBlog);
                            String thatName = getBlogNameForComparison(thatBlog);
                            return thisName.compareToIgnoreCase(thatName);
                        }
                    });
                    return !mFollowedBlogs.isSameList(mTmpFollowedBlogs);

                default:
                    return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                switch (getBlogType()) {
                    case RECOMMENDED:
                        mRecommendedBlogs = (ReaderRecommendBlogList) mTmpRecommendedBlogs.clone();
                        break;
                    case FOLLOWED:
                        mFollowedBlogs = (ReaderBlogList) mTmpFollowedBlogs.clone();
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
                return StringUtils.notNullStr(UrlUtils.getHost(blog.getUrl()));
            } else {
                return "";
            }
        }
    }

    public String getSearchFilter() {
        return mSearchFilter;
    }

    /*
     * filters the list of followed sites - pass null to show all
     */
    public void setSearchFilter(String constraint) {
        if (!StringUtils.equals(constraint, mSearchFilter)) {
            mSearchFilter = constraint;
            refresh();
        }
    }

    public boolean hasSearchFilter() {
        return !TextUtils.isEmpty(mSearchFilter);
    }
}
