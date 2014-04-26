package org.wordpress.android.ui.reader.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class ReaderRecommendedBlogAdapter extends BaseAdapter {
    public interface TagActionListener {
        public void onTagAction(ReaderTagActions.TagAction action, String tagName);
    }

    private final LayoutInflater mInflater;
    private ReaderRecommendBlogList mBlogs = new ReaderRecommendBlogList();
    private final Drawable mDrawableAdd;
    private final Drawable mDrawableRemove;

    public ReaderRecommendedBlogAdapter(Context context) {
        super();

        mInflater = LayoutInflater.from(context);
        mDrawableAdd = context.getResources().getDrawable(R.drawable.ic_content_new);
        mDrawableRemove = context.getResources().getDrawable(R.drawable.ic_content_remove);
    }

    @SuppressLint("NewApi")
    public void refresh() {
        if (mIsTaskRunning) {
            AppLog.w(T.READER, "recommended blog task is already running");
        }

        if (SysUtils.canUseExecuteOnExecutor()) {
            new LoadBlogsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new LoadBlogsTask().execute();
        }
    }

    @Override
    public int getCount() {
        return mBlogs.size();
    }

    @Override
    public Object getItem(int position) {
        return mBlogs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ReaderRecommendedBlog blog = (ReaderRecommendedBlog) getItem(position);
        BlogViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.reader_listitem_blog, parent, false);
            holder = new BlogViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (BlogViewHolder) convertView.getTag();
        }

        holder.txtTitle.setText(blog.getTitle());
        holder.txtUrl.setText(blog.getBlogDomain());
        holder.imgBlog.setImageUrl(blog.getImageUrl(), WPNetworkImageView.ImageType.AVATAR);

        // TODO: show blog detail
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReaderActivityLauncher.openUrl(v.getContext(), blog.getBlogDomain());
            }
        });

        // TODO: implement following

        return convertView;
    }

    private static class BlogViewHolder {
        private final TextView txtTitle;
        private final TextView txtUrl;
        private final TextView txtFollow;
        private final WPNetworkImageView imgBlog;

        BlogViewHolder(View view) {
            txtTitle = (TextView) view.findViewById(R.id.text_title);
            txtUrl = (TextView) view.findViewById(R.id.text_url);
            txtFollow = (TextView) view.findViewById(R.id.text_follow);
            imgBlog = (WPNetworkImageView) view.findViewById(R.id.image_blog);
        }
    }

    /*
     * AsyncTask to load recommended blogs
     */
    private boolean mIsTaskRunning = false;
    private class LoadBlogsTask extends AsyncTask<Void, Void, Boolean> {
        ReaderRecommendBlogList tmpBlogs;
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
            // TODO: exclude followed blogs
            tmpBlogs = ReaderBlogTable.getRecommendedBlogs();
            return (tmpBlogs != null && tmpBlogs.size() > 0);
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mBlogs = (ReaderRecommendBlogList)(tmpBlogs.clone());
                notifyDataSetChanged();
            }
            mIsTaskRunning = false;
        }
    }


}
