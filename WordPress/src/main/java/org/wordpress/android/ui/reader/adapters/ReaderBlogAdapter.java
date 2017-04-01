package org.wordpress.android.ui.reader.adapters;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
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

    public interface BlogClickListener {
        void onBlogClicked(Object blog);
    }

    private final ReaderBlogType mBlogType;
    private BlogClickListener mClickListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;

    private ReaderRecommendBlogList mRecommendedBlogs = new ReaderRecommendBlogList();
    private ReaderBlogList mFollowedBlogs = new ReaderBlogList();

    private String mSearchFilter;

    public ReaderBlogAdapter(ReaderBlogType blogType, String searchFilter) {
        super();
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
        if (mTaskRunningEh) {
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
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.reader_listitem_blog, parent, false);
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
                    blogHolder.txtTitle.setText(blog.getTitle());
                    blogHolder.txtDescription.setText(blog.getReason());
                    blogHolder.txtUrl.setText(UrlUtils.getHost(blog.getBlogUrl()));
                    blogHolder.imgBlog.setImageUrl(blog.getImageUrl(), WPNetworkImageView.ImageType.BLAVATAR);
                    break;

                case FOLLOWED:
                    final ReaderBlog blogInfo = mFollowedBlogs.get(position);
                    if (blogInfo.nameEh()) {
                        blogHolder.txtTitle.setText(blogInfo.getName());
                    } else {
                        blogHolder.txtTitle.setText(R.string.reader_untitled_post);
                    }
                    if (blogInfo.urlEh()) {
                        blogHolder.txtUrl.setText(UrlUtils.getHost(blogInfo.getUrl()));
                    } else if (blogInfo.feedUrlEh()) {
                        blogHolder.txtUrl.setText(UrlUtils.getHost(blogInfo.getFeedUrl()));
                    } else {
                        blogHolder.txtUrl.setText("");
                    }
                    blogHolder.imgBlog.setImageUrl(blogInfo.getImageUrl(), WPNetworkImageView.ImageType.BLAVATAR);
                    break;
            }

            if (mClickListener != null) {
                blogHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int clickedPosition = blogHolder.getAdapterPosition();
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
        private final TextView txtTitle;
        private final TextView txtDescription;
        private final TextView txtUrl;
        private final WPNetworkImageView imgBlog;

        public BlogViewHolder(View view) {
            super(view);

            txtTitle = (TextView) view.findViewById(R.id.text_title);
            txtDescription = (TextView) view.findViewById(R.id.text_description);
            txtUrl = (TextView) view.findViewById(R.id.text_url);
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

    private boolean mTaskRunningEh = false;
    private class LoadBlogsTask extends AsyncTask<Void, Void, Boolean> {
        ReaderRecommendBlogList tmpRecommendedBlogs;
        ReaderBlogList tmpFollowedBlogs;

        @Override
        protected void onPreExecute() {
            mTaskRunningEh = true;
        }

        @Override
        protected void onCancelled() {
            mTaskRunningEh = false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            switch (getBlogType()) {
                case RECOMMENDED:
                    tmpRecommendedBlogs = ReaderBlogTable.getRecommendedBlogs();
                    return !mRecommendedBlogs.sameListEh(tmpRecommendedBlogs);

                case FOLLOWED:
                    tmpFollowedBlogs = new ReaderBlogList();
                    ReaderBlogList allFollowedBlogs = ReaderBlogTable.getFollowedBlogs();
                    if (searchFilterEh()) {
                        String query = mSearchFilter.toLowerCase();
                        for (ReaderBlog blog: allFollowedBlogs) {
                            if (blog.getName().toLowerCase().contains(query)) {
                                tmpFollowedBlogs.add(blog);
                            } else if (UrlUtils.getHost(blog.getUrl()).toLowerCase().contains(query)) {
                                tmpFollowedBlogs.add(blog);
                            }
                        }
                    } else {
                        tmpFollowedBlogs.addAll(allFollowedBlogs);
                    }
                    // sort followed blogs by name/domain to match display
                    Collections.sort(tmpFollowedBlogs, new Comparator<ReaderBlog>() {
                        @Override
                        public int compare(ReaderBlog thisBlog, ReaderBlog thatBlog) {
                            String thisName = getBlogNameForComparison(thisBlog);
                            String thatName = getBlogNameForComparison(thatBlog);
                            return thisName.compareToIgnoreCase(thatName);
                        }
                    });
                    return !mFollowedBlogs.sameListEh(tmpFollowedBlogs);

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
                        break;
                }
                notifyDataSetChanged();
            }

            mTaskRunningEh = false;

            if (mDataLoadedListener != null) {
                mDataLoadedListener.onDataLoaded(isEmpty());
            }
        }

        private String getBlogNameForComparison(ReaderBlog blog) {
            if (blog == null) {
                return "";
            } else if (blog.nameEh()) {
                return blog.getName();
            } else if (blog.urlEh()) {
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

    public boolean searchFilterEh() {
        return !TextUtils.isEmpty(mSearchFilter);
    }

}
