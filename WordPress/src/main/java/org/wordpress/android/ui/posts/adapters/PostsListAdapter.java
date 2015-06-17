package org.wordpress.android.ui.posts.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.posts.PostsListFragment;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for Posts/Pages list
 */
public class PostsListAdapter extends RecyclerView.Adapter<PostsListAdapter.PostViewHolder> {

    private final boolean mIsPage;
    private final LayoutInflater mLayoutInflater;
    private OnLoadMoreListener mOnLoadMoreListener;
    private OnPostsLoadedListener mOnPostsLoadedListener;
    private OnPostSelectedListener mOnPostSelectedListener;
    private int mSelectedPosition = -1;
    private boolean mShowSelection;
    private List<PostsListPost> mPosts = new ArrayList<>();

    public PostsListAdapter(Context context, boolean isPage) {
        mIsPage = isPage;
        mLayoutInflater = LayoutInflater.from(context);
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;
    }

    public void setOnPostsLoadedListener(OnPostsLoadedListener listener) {
        mOnPostsLoadedListener = listener;
    }

    public void setOnPostSelectedListener(OnPostSelectedListener listener) {
        mOnPostSelectedListener = listener;
    }

    private void setPosts(List<PostsListPost> postsList) {
        if (postsList != null) {
            this.mPosts = postsList;
        }
    }

    public PostsListPost getItem(int position) {
        if (isValidPosition(position)) {
            return mPosts.get(position);
        }
        return null;
    }

    private boolean isValidPosition(int position) {
        return (position >= 0 && position < mPosts.size());
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.post_cardview, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PostViewHolder holder, final int position) {
        PostsListPost post = mPosts.get(position);
        Context context = holder.itemView.getContext();

        if (post.hasTitle()) {
            holder.txtTitle.setText(post.getTitle());
        } else {
            holder.txtTitle.setText("(" + context.getResources().getText(R.string.untitled) + ")");
        }

        if (post.hasExcerpt()) {
            holder.txtExcerpt.setVisibility(View.VISIBLE);
            holder.txtExcerpt.setText(post.getExcerpt());
        } else {
            holder.txtExcerpt.setVisibility(View.GONE);
        }

        if (post.hasFeaturedImage()) {
            holder.imgFeatured.setVisibility(View.VISIBLE);
            // TODO: photonize this
            holder.imgFeatured.setImageUrl(post.getFeaturedImageUrl(), WPNetworkImageView.ImageType.PHOTO);
        } else {
            holder.imgFeatured.setVisibility(View.GONE);
        }

        if (post.isLocalDraft()) {
            holder.txtDate.setVisibility(View.GONE);
        } else {
            holder.txtDate.setText(post.getFormattedDate());
            holder.txtDate.setVisibility(View.VISIBLE);
        }

        if ((post.getStatusEnum() == PostStatus.PUBLISHED) && !post.isLocalDraft() && !post.hasLocalChanges()) {
            holder.txtStatus.setVisibility(View.GONE);
        } else {
            final String status;
            holder.txtStatus.setVisibility(View.VISIBLE);
            if (post.isUploading()) {
                status = context.getResources().getString(R.string.post_uploading);
            } else if (post.isLocalDraft()) {
                status = context.getResources().getString(R.string.local_draft);
            } else if (post.hasLocalChanges()) {
                status = context.getResources().getString(R.string.local_changes);
            } else {
                switch (post.getStatusEnum()) {
                    case DRAFT:
                        status = context.getResources().getString(R.string.draft);
                        break;
                    case PRIVATE:
                        status = context.getResources().getString(R.string.post_private);
                        break;
                    case PENDING:
                        status = context.getResources().getString(R.string.pending_review);
                        break;
                    case SCHEDULED:
                        status = context.getResources().getString(R.string.scheduled);
                        break;
                    default:
                        status = "";
                        break;
                }
            }

            if (post.isLocalDraft() || post.getStatusEnum() == PostStatus.DRAFT || post.hasLocalChanges() ||
                    post.isUploading()) {
                holder.txtStatus.setTextColor(context.getResources().getColor(R.color.orange_fire));
            } else {
                holder.txtStatus.setTextColor(context.getResources().getColor(R.color.grey_darken_10));
            }
            // Make status upper-case and add line break to stack vertically
            holder.txtStatus.setText(status.toUpperCase(Locale.getDefault()).replace(" ", "\n"));
        }

        // load more posts when we near the end
        if (mOnLoadMoreListener != null && position >= getItemCount() - 1
                && position >= PostsListFragment.POSTS_REQUEST_COUNT - 1) {
            mOnLoadMoreListener.onLoadMore();
        }

        if (mShowSelection) {
            holder.itemView.setSelected(position == mSelectedPosition);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mShowSelection) {
                    setSelectedPosition(position);
                }
                if (mOnPostSelectedListener != null) {
                    PostsListPost selectedPost = getItem(position);
                    if (selectedPost != null) {
                        mOnPostSelectedListener.onPostSelected(selectedPost);
                    }
                }
            }
        });
    }

    public void setShowSelection(boolean showSelection) {
        mShowSelection = showSelection;
    }

    public void setSelectedPosition(int position) {
        if (position == mSelectedPosition) {
            return;
        }
        if (isValidPosition(mSelectedPosition)) {
            notifyItemChanged(mSelectedPosition);
        }
        mSelectedPosition = position;
        if (isValidPosition(mSelectedPosition)) {
            notifyItemChanged(mSelectedPosition);
        }
    }

    @Override
    public long getItemId(int position) {
        return mPosts.get(position).getPostId();
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
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

    private boolean postsListMatch(List<PostsListPost> newPostsList) {
        if (newPostsList == null || newPostsList.size() == 0 || mPosts == null || mPosts.size() != newPostsList.size()) {
            return false;
        }

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
            if (newPost.isUploading() != currentPost.isUploading())
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

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public interface OnPostSelectedListener {
        void onPostSelected(PostsListPost post);
    }

    public interface OnPostsLoadedListener {
        void onPostsLoaded(int postCount);
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final TextView txtExcerpt;
        private final TextView txtDate;
        private final TextView txtStatus;
        private final WPNetworkImageView imgFeatured;

        public PostViewHolder(View view) {
            super(view);
            txtTitle = (TextView) view.findViewById(R.id.text_title);
            txtExcerpt = (TextView) view.findViewById(R.id.text_excerpt);
            txtDate = (TextView) view.findViewById(R.id.text_date);
            txtStatus = (TextView) view.findViewById(R.id.text_status);
            imgFeatured = (WPNetworkImageView) view.findViewById(R.id.image_featured);
        }
    }

    private class LoadPostsTask extends AsyncTask<Void, Void, Boolean> {
        List<PostsListPost> loadedPosts;

        @Override
        protected Boolean doInBackground(Void... nada) {
            loadedPosts = WordPress.wpDB.getPostsListPosts(WordPress.getCurrentLocalTableBlogId(), mIsPage);
            return !postsListMatch(loadedPosts);

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
}
