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
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.ui.posts.PostsListFragment;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.Locale;

/**
 * Adapter for Posts/Pages list
 */
public class PostsListAdapter extends RecyclerView.Adapter<PostsListAdapter.PostViewHolder> {

    public enum PostButton {
        EDIT,
        VIEW,
        STATS,
        TRASH
    }

    public interface OnPostButtonClickListener {
        void onPostButtonClicked(PostButton button, PostsListPost post);
    }

    private OnLoadMoreListener mOnLoadMoreListener;
    private OnPostsLoadedListener mOnPostsLoadedListener;
    private OnPostSelectedListener mOnPostSelectedListener;
    private OnPostButtonClickListener mOnPostButtonClickListener;

    private int mSelectedPosition = -1;
    private final int mLocalTableBlogId;
    private final int mPhotonWidth;
    private final int mPhotonHeight;

    private boolean mShowSelection;
    private final boolean mIsPage;
    private final boolean mIsPrivateBlog;

    private final PostsListPostList mPosts = new PostsListPostList();
    private final LayoutInflater mLayoutInflater;

    public PostsListAdapter(Context context, int localTableBlogId, boolean isPage, boolean isPrivateBlog) {
        mLocalTableBlogId = localTableBlogId;
        mIsPage = isPage;
        mIsPrivateBlog = isPrivateBlog;
        mLayoutInflater = LayoutInflater.from(context);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int cardSpacing = context.getResources().getDimensionPixelSize(R.dimen.content_margin);
        mPhotonWidth = displayWidth - (cardSpacing * 2);
        mPhotonHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height);
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

    public void setOnPostButtonClickListener(OnPostButtonClickListener listener) {
        mOnPostButtonClickListener = listener;
    }

    private void setPosts(PostsListPostList postsList) {
        mPosts.clear();
        if (postsList != null) {
            mPosts.addAll(postsList);
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
            final String imageUrl = ReaderUtils.getResizedImageUrl(post.getFeaturedImageUrl(),
                    mPhotonWidth, mPhotonHeight, mIsPrivateBlog);
            holder.imgFeatured.setVisibility(View.VISIBLE);
            holder.imgFeatured.setImageUrl(imageUrl, WPNetworkImageView.ImageType.PHOTO);
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

        View.OnClickListener btnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postButtonClicked(v, position);
            }
        };
        holder.btnEdit.setOnClickListener(btnClickListener);
        holder.btnView.setOnClickListener(btnClickListener);
        holder.btnStats.setOnClickListener(btnClickListener);
        holder.btnTrash.setOnClickListener(btnClickListener);

        // local drafts don't have stats and can't be viewed on the web
        holder.btnView.setVisibility(post.isLocalDraft() ? View.INVISIBLE : View.VISIBLE);
        holder.btnStats.setVisibility(post.isLocalDraft() ? View.INVISIBLE : View.VISIBLE);

        // load more posts when we near the end
        if (mOnLoadMoreListener != null && position >= getItemCount() - 1
                && position >= PostsListFragment.POSTS_REQUEST_COUNT - 1) {
            mOnLoadMoreListener.onLoadMore();
        }

        // highlight selected post in dual-pane
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

    private void postButtonClicked(View view, int position) {
        if (mOnPostButtonClickListener == null) {
            return;
        }

        PostsListPost post = getItem(position);
        if (post == null) {
            return;
        }

        if (view.getId() == R.id.btn_edit) {
            mOnPostButtonClickListener.onPostButtonClicked(PostButton.EDIT, post);
        } else if (view.getId() == R.id.btn_view) {
            mOnPostButtonClickListener.onPostButtonClicked(PostButton.VIEW, post);
        } else if (view.getId() == R.id.btn_stats) {
            mOnPostButtonClickListener.onPostButtonClicked(PostButton.STATS, post);
        } else if (view.getId() == R.id.btn_trash) {
            mOnPostButtonClickListener.onPostButtonClicked(PostButton.TRASH, post);
        }
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
        new LoadPostsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public int getRemotePostCount() {
        if (mPosts == null) {
            return 0;
        }

        int remotePostCount = 0;
        for (PostsListPost post : mPosts) {
            if (!post.isLocalDraft())
                remotePostCount++;
        }

        return remotePostCount;
    }

    public void removePost(PostsListPost post) {
        int position = mPosts.indexOfPost(post);
        if (position == -1) {
            return;
        }
        mPosts.remove(position);
        notifyItemRemoved(position);
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

        private final TextView btnEdit;
        private final TextView btnView;
        private final TextView btnStats;
        private final TextView btnTrash;

        private final WPNetworkImageView imgFeatured;

        public PostViewHolder(View view) {
            super(view);

            txtTitle = (TextView) view.findViewById(R.id.text_title);
            txtExcerpt = (TextView) view.findViewById(R.id.text_excerpt);
            txtDate = (TextView) view.findViewById(R.id.text_date);
            txtStatus = (TextView) view.findViewById(R.id.text_status);

            btnEdit = (TextView) view.findViewById(R.id.btn_edit);
            btnView = (TextView) view.findViewById(R.id.btn_view);
            btnStats = (TextView) view.findViewById(R.id.btn_stats);
            btnTrash = (TextView) view.findViewById(R.id.btn_trash);

            imgFeatured = (WPNetworkImageView) view.findViewById(R.id.image_featured);
        }
    }

    private class LoadPostsTask extends AsyncTask<Void, Void, Boolean> {
        PostsListPostList loadedPosts;

        @Override
        protected Boolean doInBackground(Void... nada) {
            loadedPosts = WordPress.wpDB.getPostsListPosts(mLocalTableBlogId, mIsPage);
            return !loadedPosts.isSameList(mPosts);

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
