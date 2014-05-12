package org.wordpress.android.ui.posts.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.posts.PostsListFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for Posts/Pages list
 */
public class PostsListAdapter extends BaseAdapter {
    public static interface OnLoadMoreListener {
        public void onLoadMore();
    }

    public static interface OnPostsLoadedListener {
        public void onPostsLoaded(int postCount);
    }

    private final OnLoadMoreListener mOnLoadMoreListener;
    private final OnPostsLoadedListener mOnPostsLoadedListener;
    private Context mContext;
    private boolean mIsPage;
    private LayoutInflater mLayoutInflater;

    private List<PostsListPost> mPosts = new ArrayList<PostsListPost>();


    public PostsListAdapter(Context context, boolean isPage, OnLoadMoreListener onLoadMoreListener, OnPostsLoadedListener onPostsLoadedListener) {
        mContext = context;
        mIsPage = isPage;
        mOnLoadMoreListener = onLoadMoreListener;
        mOnPostsLoadedListener = onPostsLoadedListener;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    public List<PostsListPost> getPosts() {
        return mPosts;
    }

    public void setPosts(List<PostsListPost> postsList) {
        if (postsList != null)
            this.mPosts = postsList;
    }

    @Override
    public int getCount() {
        return mPosts.size();
    }

    @Override
    public Object getItem(int position) {
        return mPosts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mPosts.get(position).getPostId();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        PostsListPost post = mPosts.get(position);
        PostViewWrapper wrapper;
        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.post_list_row, parent, false);
            wrapper = new PostViewWrapper(view);
            view.setTag(wrapper);
        } else {
            wrapper = (PostViewWrapper) view.getTag();
        }

        String date = post.getFormattedDate();

        String titleText = post.getTitle();
        if (titleText.equals(""))
            titleText = "(" + mContext.getResources().getText(R.string.untitled) + ")";
        wrapper.getTitle().setText(titleText);

        if (post.isLocalDraft()) {
            wrapper.getDate().setVisibility(View.GONE);
        } else {
            wrapper.getDate().setText(date);
            wrapper.getDate().setVisibility(View.VISIBLE);
        }

        String formattedStatus = "";
        if ((post.getStatusEnum() == PostStatus.PUBLISHED) && !post.isLocalDraft() && !post.hasLocalChanges()) {
            wrapper.getStatus().setVisibility(View.GONE);
        } else {
            wrapper.getStatus().setVisibility(View.VISIBLE);
            if (post.isLocalDraft()) {
                formattedStatus = mContext.getResources().getString(R.string.local_draft);
            } else if (post.hasLocalChanges()) {
                formattedStatus = mContext.getResources().getString(R.string.local_changes);
            } else {
                switch (post.getStatusEnum()) {
                    case DRAFT:
                        formattedStatus = mContext.getResources().getString(R.string.draft);
                        break;
                    case PRIVATE:
                        formattedStatus = mContext.getResources().getString(R.string.post_private);
                        break;
                    case PENDING:
                        formattedStatus = mContext.getResources().getString(R.string.pending_review);
                        break;
                    case SCHEDULED:
                        formattedStatus = mContext.getResources().getString(R.string.scheduled);
                        break;
                    default:
                        break;
                }
            }

            // Set post status TextView color
            if (post.isLocalDraft() || post.getStatusEnum() == PostStatus.DRAFT || post.hasLocalChanges()) {
                wrapper.getStatus().setTextColor(mContext.getResources().getColor(R.color.orange_dark));
            } else {
                wrapper.getStatus().setTextColor(mContext.getResources().getColor(R.color.grey_medium));
            }

            // Make status upper-case and add line break to stack vertically
            formattedStatus = formattedStatus.toUpperCase(Locale.getDefault()).replace(" ", "\n");
            wrapper.getStatus().setText(formattedStatus);
        }

        // load more posts when we near the end
        if (mOnLoadMoreListener != null && position >= getCount() - 1
                && position >= PostsListFragment.POSTS_REQUEST_COUNT - 1) {
            mOnLoadMoreListener.onLoadMore();
        }

        return view;
    }

    public void loadPosts() {
        if (WordPress.getCurrentBlog() == null) {
            return;
        }

        // load posts from db
        new LoadPostsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void clear() {
        if (mPosts.size() > 0) {
            mPosts.clear();
            notifyDataSetChanged();
        }
    }

    class PostViewWrapper {
        View base;
        TextView title = null;
        TextView date = null;
        TextView status = null;

        PostViewWrapper(View base) {
            this.base = base;
        }

        TextView getTitle() {
            if (title == null) {
                title = (TextView) base.findViewById(R.id.post_list_title);
            }
            return (title);
        }

        TextView getDate() {
            if (date == null) {
                date = (TextView) base.findViewById(R.id.post_list_date);
            }
            return (date);
        }

        TextView getStatus() {
            if (status == null) {
                status = (TextView) base.findViewById(R.id.post_list_status);
            }
            return (status);
        }
    }

    private class LoadPostsTask extends AsyncTask <Void, Void, Boolean> {
        List<PostsListPost> loadedPosts;

        @Override
        protected Boolean doInBackground(Void... nada) {
            loadedPosts = WordPress.wpDB.getPostsListPosts(WordPress.getCurrentLocalTableBlogId(), mIsPage);
            if (postsListMatch(loadedPosts)) {
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                setPosts(loadedPosts);
                notifyDataSetChanged();

                if (mOnPostsLoadedListener != null && mPosts != null) {
                    mOnPostsLoadedListener.onPostsLoaded(mPosts.size());
                }
            }
        }
    }

    public boolean postsListMatch(List<PostsListPost> newPostsList) {
        if (newPostsList == null || newPostsList.size() == 0 || mPosts == null || mPosts.size() != newPostsList.size())
            return false;

        for (int i = 0; i < newPostsList.size(); i++) {
            PostsListPost newPost = newPostsList.get(i);
            PostsListPost currentPost = mPosts.get(i);

            if (newPost.getPostId() != currentPost.getPostId())
                return false;
            if (!newPost.getTitle().equals(currentPost.getTitle()))
                return false;
            if (newPost.getDateCreatedGmt() != currentPost.getDateCreatedGmt())
                return false;
            if (!newPost.getOriginalStatus().equals(currentPost.getOriginalStatus()))
                return false;
            if (newPost.isLocalDraft() != currentPost.isLocalDraft())
                return false;
            if (newPost.hasLocalChanges() != currentPost.hasLocalChanges())
                return false;
        }

        return true;
    }

    public int getRemotePostCount() {
        if (mPosts == null)
            return 0;

        int remotePostCount = 0;
        for (PostsListPost post : mPosts) {
            if (!post.isLocalDraft())
                remotePostCount++;
        }

        return remotePostCount;
    }
}
