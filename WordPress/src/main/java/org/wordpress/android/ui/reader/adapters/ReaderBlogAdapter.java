package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
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
import org.wordpress.android.ui.reader.ReaderConstants;
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
public class ReaderBlogAdapter extends BaseAdapter {
    public enum ReaderBlogType {RECOMMENDED, FOLLOWED}

    private final LayoutInflater mInflater;
    private final ReaderBlogType mBlogType;

    private ReaderRecommendBlogList mRecommendedBlogs = new ReaderRecommendBlogList();
    private ReaderBlogList mFollowedBlogs = new ReaderBlogList();

    public ReaderBlogAdapter(Context context, ReaderBlogType blogType) {
        super();
        mInflater = LayoutInflater.from(context);
        mBlogType = blogType;
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

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public long getItemId(int position) {
        return position;
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

        switch (getBlogType()) {
            case RECOMMENDED:
                final ReaderRecommendedBlog blog = (ReaderRecommendedBlog) getItem(position);
                holder.txtTitle.setText(blog.getTitle());
                holder.txtDescription.setText(blog.getReason());
                holder.txtUrl.setText(UrlUtils.getDomainFromUrl(blog.getBlogUrl()));
                holder.imgBlog.setImageUrl(blog.getImageUrl(), WPNetworkImageView.ImageType.SITE_AVATAR);
                break;

            case FOLLOWED:
                final ReaderBlog blogInfo = (ReaderBlog) getItem(position);
                String domain = UrlUtils.getDomainFromUrl(blogInfo.getUrl());
                if (blogInfo.hasName()) {
                    holder.txtTitle.setText(blogInfo.getName());
                } else {
                    holder.txtTitle.setText(domain);
                }
                holder.txtUrl.setText(domain);
                holder.imgBlog.setImageUrl(blogInfo.getImageUrl(), WPNetworkImageView.ImageType.SITE_AVATAR);
                break;
        }

        return convertView;
    }

    private class BlogViewHolder {
        private final TextView txtTitle;
        private final TextView txtDescription;
        private final TextView txtUrl;
        private final WPNetworkImageView imgBlog;

        BlogViewHolder(View view) {
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
