package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderAnim;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.ReaderInterfaces;
import org.wordpress.android.ui.reader.ReaderTypes;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.views.ReaderIconCountView;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class ReaderPostRecycler extends RecyclerView.Adapter<ReaderPostRecycler.ReaderPostViewHolder> {

    private ReaderTag mCurrentTag;
    private long mCurrentBlogId;

    private final int mPhotonWidth;
    private final int mPhotonHeight;
    private final int mAvatarSz;
    private final int mMarginLarge;

    private boolean mCanRequestMorePosts = false;

    private final ReaderTypes.ReaderPostListType mPostListType;
    private ReaderPostList mPosts = new ReaderPostList();

    private ReaderInterfaces.OnTagSelectedListener mOnTagSelectedListener;
    private ReaderInterfaces.OnPostPopupListener mOnPostPopupListener;
    private final ReaderInterfaces.RequestReblogListener mReblogListener;
    private final ReaderInterfaces.DataLoadedListener mDataLoadedListener;
    private final ReaderActions.DataRequestedListener mDataRequestedListener;

    // the large "tbl_posts.text" column is unused here, so skip it when querying
    private static final boolean EXCLUDE_TEXT_COLUMN = true;
    private static final int MAX_ROWS = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY;

    static class ReaderPostViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final TextView txtText;
        private final TextView txtBlogName;
        private final TextView txtDate;
        private final TextView txtFollow;
        private final TextView txtTag;

        private final ReaderIconCountView commentCount;
        private final ReaderIconCountView likeCount;

        private final ImageView imgBtnReblog;
        private final ImageView imgMore;

        private final WPNetworkImageView imgFeatured;
        private final WPNetworkImageView imgAvatar;

        private final ViewGroup layoutPostHeader;

        public ReaderPostViewHolder(View itemView) {
            super(itemView);

            txtTitle = (TextView) itemView.findViewById(R.id.text_title);
            txtText = (TextView) itemView.findViewById(R.id.text_excerpt);
            txtBlogName = (TextView) itemView.findViewById(R.id.text_blog_name);
            txtDate = (TextView) itemView.findViewById(R.id.text_date);
            txtFollow = (TextView) itemView.findViewById(R.id.text_follow);
            txtTag = (TextView) itemView.findViewById(R.id.text_tag);

            commentCount = (ReaderIconCountView) itemView.findViewById(R.id.count_comments);
            likeCount = (ReaderIconCountView) itemView.findViewById(R.id.count_likes);

            imgFeatured = (WPNetworkImageView) itemView.findViewById(R.id.image_featured);
            imgAvatar = (WPNetworkImageView) itemView.findViewById(R.id.image_avatar);

            imgBtnReblog = (ImageView) itemView.findViewById(R.id.image_reblog_btn);
            imgMore = (ImageView) itemView.findViewById(R.id.image_more);

            layoutPostHeader = (ViewGroup) itemView.findViewById(R.id.layout_post_header);
        }
    }

    @Override
    public ReaderPostViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.reader_listitem_post_excerpt, viewGroup, false);
        return new ReaderPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ReaderPostViewHolder holder, final int position) {
        final ReaderPost post = mPosts.get(position);
        ReaderTypes.ReaderPostListType postListType = getPostListType();

        holder.txtTitle.setText(post.getTitle());
        holder.txtDate.setText(DateTimeUtils.javaDateToTimeSpan(post.getDatePublished()));

        // hide the post header (avatar, blog name & follow button) if we're showing posts
        // in a specific blog
        if (postListType == ReaderTypes.ReaderPostListType.BLOG_PREVIEW) {
            holder.layoutPostHeader.setVisibility(View.GONE);
        } else {
            holder.layoutPostHeader.setVisibility(View.VISIBLE);
            holder.imgAvatar.setImageUrl(post.getPostAvatarForDisplay(mAvatarSz), WPNetworkImageView.ImageType.AVATAR);
            if (post.hasBlogName()) {
                holder.txtBlogName.setText(post.getBlogName());
            } else if (post.hasAuthorName()) {
                holder.txtBlogName.setText(post.getAuthorName());
            } else {
                holder.txtBlogName.setText(null);
            }

            // follow/following
            ReaderUtils.showFollowStatus(holder.txtFollow, post.isFollowedByCurrentUser);
            holder.txtFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollow(holder, position, post);
                }
            });

            // tapping header shows blog preview unless this post is from an external feed
            if (!post.isExternal) {
                holder.layoutPostHeader.setEnabled(true);
                holder.layoutPostHeader.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ReaderActivityLauncher.showReaderBlogPreview(view.getContext(), post.blogId, post.getBlogUrl());
                    }
                });
            } else {
                holder.layoutPostHeader.setOnClickListener(null);
                holder.layoutPostHeader.setEnabled(false);
            }
        }

        if (post.hasExcerpt()) {
            holder.txtText.setVisibility(View.VISIBLE);
            holder.txtText.setText(post.getExcerpt());
        } else {
            holder.txtText.setVisibility(View.GONE);
        }

        final int titleMargin;
        if (post.hasFeaturedImage()) {
            final String imageUrl = post.getFeaturedImageForDisplay(mPhotonWidth, mPhotonHeight);
            holder.imgFeatured.setImageUrl(imageUrl, WPNetworkImageView.ImageType.PHOTO);
            holder.imgFeatured.setVisibility(View.VISIBLE);
            titleMargin = mMarginLarge;
        } else if (post.hasFeaturedVideo()) {
            holder.imgFeatured.setVideoUrl(post.postId, post.getFeaturedVideo());
            holder.imgFeatured.setVisibility(View.VISIBLE);
            titleMargin = mMarginLarge;
        } else {
            holder.imgFeatured.setVisibility(View.GONE);
            titleMargin = (holder.layoutPostHeader.getVisibility() == View.VISIBLE ? 0 : mMarginLarge);
        }

        // set the top margin of the title based on whether there's a featured image and post header
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.txtTitle.getLayoutParams();
        params.topMargin = titleMargin;

        // show the best tag for this post
        final String tagToDisplay = (mCurrentTag != null ? post.getTagForDisplay(mCurrentTag.getTagName()) : null);
        if (!TextUtils.isEmpty(tagToDisplay)) {
            holder.txtTag.setText(tagToDisplay);
            holder.txtTag.setVisibility(View.VISIBLE);
            holder.txtTag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnTagSelectedListener != null) {
                        mOnTagSelectedListener.onTagSelected(tagToDisplay);
                    }
                }
            });
        } else {
            holder.txtTag.setVisibility(View.GONE);
            holder.txtTag.setOnClickListener(null);
        }

        // likes, comments & reblogging - supported by wp posts only
        boolean showLikes = post.isWP() && post.isLikesEnabled;
        boolean showComments = post.isWP() && (post.isCommentsOpen || post.numReplies > 0);

        if (showLikes || showComments) {
            showCounts(holder, post, false);
        }

        if (showLikes) {
            holder.likeCount.setSelected(post.isLikedByCurrentUser);
            holder.likeCount.setVisibility(View.VISIBLE);
            holder.likeCount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleLike(v.getContext(), holder, position, post);
                }
            });
        } else {
            holder.likeCount.setVisibility(View.GONE);
            holder.likeCount.setOnClickListener(null);
        }

        if (showComments) {
            holder.commentCount.setVisibility(View.VISIBLE);
            holder.commentCount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.showReaderComments(v.getContext(), post);
                }
            });
        } else {
            holder.commentCount.setVisibility(View.GONE);
            holder.commentCount.setOnClickListener(null);
        }

        if (post.canReblog()) {
            showReblogStatus(holder.imgBtnReblog, post.isRebloggedByCurrentUser);
            holder.imgBtnReblog.setVisibility(View.VISIBLE);
            if (!post.isRebloggedByCurrentUser) {
                holder.imgBtnReblog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ReaderAnim.animateReblogButton(holder.imgBtnReblog);
                        if (mReblogListener != null) {
                            mReblogListener.onRequestReblog(post, v);
                        }
                    }
                });
            } else {
                holder.imgBtnReblog.setOnClickListener(null);
            }
        } else {
            // use INVISIBLE rather than GONE to ensure container maintains the same height
            holder.imgBtnReblog.setVisibility(View.INVISIBLE);
            holder.imgBtnReblog.setOnClickListener(null);
        }

        // more menu with "block this blog" only shows for public wp posts in followed tags
        if (post.isWP() && !post.isPrivate && postListType == ReaderTypes.ReaderPostListType.TAG_FOLLOWED) {
            holder.imgMore.setVisibility(View.VISIBLE);
            holder.imgMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnPostPopupListener != null) {
                        mOnPostPopupListener.onShowPostPopup(view, post);
                    }
                }
            });
        } else {
            holder.imgMore.setVisibility(View.GONE);
            holder.imgMore.setOnClickListener(null);
        }

        // if we're nearing the end of the posts, fire request to load more
        if (mCanRequestMorePosts && mDataRequestedListener != null && (position >= getItemCount()-1)) {
            mDataRequestedListener.onRequestData();
        }
    }

    // ********************************************************************************************

     public ReaderPostRecycler(Context context,
                               ReaderTypes.ReaderPostListType postListType,
                               ReaderInterfaces.RequestReblogListener reblogListener,
                               ReaderInterfaces.DataLoadedListener dataLoadedListener,
                               ReaderActions.DataRequestedListener dataRequestedListener) {
        super();

        mPostListType = postListType;
        mReblogListener = reblogListener;
        mDataLoadedListener = dataLoadedListener;
        mDataRequestedListener = dataRequestedListener;

        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
        mMarginLarge = context.getResources().getDimensionPixelSize(R.dimen.margin_large);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int listMargin = context.getResources().getDimensionPixelSize(R.dimen.reader_list_margin);
        mPhotonWidth = displayWidth - (listMargin * 2);
        mPhotonHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height);
    }

    public void setOnTagSelectedListener(ReaderInterfaces.OnTagSelectedListener listener) {
        mOnTagSelectedListener = listener;
    }

    public void setOnPostPopupListener(ReaderInterfaces.OnPostPopupListener onPostPopupListener) {
        mOnPostPopupListener = onPostPopupListener;
    }

    ReaderTypes.ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
    }

    // used when the viewing tagged posts
    public void setCurrentTag(ReaderTag tag) {
        if (!ReaderTag.isSameTag(tag, mCurrentTag)) {
            mCurrentTag = tag;
            reload();
        }
    }

    public boolean isCurrentTag(ReaderTag tag) {
        return ReaderTag.isSameTag(tag, mCurrentTag);
    }

    // used when the list type is ReaderPostListType.BLOG_PREVIEW
    public void setCurrentBlog(long blogId) {
        if (blogId != mCurrentBlogId) {
            mCurrentBlogId = blogId;
            reload();
        }
    }

    private void clear() {
        if (!mPosts.isEmpty()) {
            mPosts.clear();
            notifyDataSetChanged();
        }
    }

    public void refresh() {
        loadPosts();
    }

    /*
     * same as refresh() above but first clears the existing posts
     */
    public void reload() {
        clear();
        loadPosts();
    }

    /*
     * remove a single post at the passed position
     */
    public void removePost(int position) {
        if (isValidPosition(position)) {
            mPosts.remove(position);
            notifyDataSetChanged();
        }
    }

    /*
     * reload a single post
     */
    public void reloadPost(ReaderPost post) {
        int index = indexOfPost(post);
        if (index == -1) {
            return;
        }

        final ReaderPost updatedPost = ReaderPostTable.getPost(post.blogId, post.postId, true);
        if (updatedPost != null) {
            mPosts.set(index, updatedPost);
            notifyDataSetChanged();
        }
    }

    public int indexOfPost(ReaderPost post) {
        return mPosts.indexOfPost(post);
    }

    /*
     * sets the follow status of each post in the passed blog
     */
    void updateFollowStatusOnPostsForBlog(long blogId, String blogUrl, boolean followStatus) {
        if (isEmpty()) {
            return;
        }

        boolean hasBlogId = (blogId != 0);
        boolean hasBlogUrl = !TextUtils.isEmpty(blogUrl);
        if (!hasBlogId && !hasBlogUrl) {
            return;
        }

        boolean isChanged = false;
        for (ReaderPost post: mPosts) {
            boolean isMatched = (hasBlogId ? (blogId == post.blogId) : blogUrl.equals(post.getBlogUrl()));
            if (isMatched) {
                post.isFollowedByCurrentUser = followStatus;
                isChanged = true;
            }
        }
        if (isChanged) {
            notifyDataSetChanged();
        }
    }

    private void loadPosts() {
        if (mIsTaskRunning) {
            AppLog.w(AppLog.T.READER, "reader posts task already running");
            return;
        }
        new LoadPostsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    public boolean isEmpty() {
        return (getItemCount() == 0);
    }

    boolean isValidPosition(int position) {
        return (position >= 0 && position < getItemCount());
    }

    @Override
    public long getItemId(int position) {
        return mPosts.get(position).getStableId();
    }

    /*
     * shows like & comment count
     */
    private void showCounts(ReaderPostViewHolder holder, ReaderPost post, boolean animateChanges) {
        holder.likeCount.setCount(post.numLikes, animateChanges);

        if (post.numReplies > 0 || post.isCommentsOpen) {
            holder.commentCount.setCount(post.numReplies, animateChanges);
            holder.commentCount.setVisibility(View.VISIBLE);
        } else {
            holder.commentCount.setVisibility(View.GONE);
        }
    }

    /*
     * triggered when user taps the like button (textView)
     */
    private void toggleLike(Context context, ReaderPostViewHolder holder, int position, ReaderPost post) {
        boolean isCurrentlyLiked = ReaderPostTable.isPostLikedByCurrentUser(post);
        boolean isAskingToLike = !isCurrentlyLiked;
        ReaderAnim.animateLikeButton(holder.likeCount.getImageView(), isAskingToLike);

        if (!ReaderPostActions.performLikeAction(post, isAskingToLike)) {
            ToastUtils.showToast(context, R.string.reader_toast_err_generic);
            return;
        }

        if (isAskingToLike) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_LIKED_ARTICLE);
        }

        // update post in array and on screen
        ReaderPost updatedPost = ReaderPostTable.getPost(post.blogId, post.postId, true);
        mPosts.set(position, updatedPost);
        holder.likeCount.setSelected(updatedPost.isLikedByCurrentUser);
        showCounts(holder, updatedPost, true);
    }

    /*
     * triggered when user taps the follow button
     */
    private void toggleFollow(final ReaderPostViewHolder holder, int position, ReaderPost post) {
        ReaderAnim.animateFollowButton(holder.txtFollow);
        final boolean isAskingToFollow = !post.isFollowedByCurrentUser;

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!succeeded) {
                    Context context = holder.txtTitle.getContext();
                    int resId = (isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog);
                    ToastUtils.showToast(context, resId);
                    ReaderUtils.showFollowStatus(holder.txtFollow, !isAskingToFollow);
                }
            }
        };

        if (!ReaderBlogActions.performFollowAction(post, isAskingToFollow, actionListener)) {
            return;
        }

        ReaderPost updatedPost = ReaderPostTable.getPost(post.blogId, post.postId, true);
        if (updatedPost != null) {
            mPosts.set(position, updatedPost);
        }

        ReaderUtils.showFollowStatus(holder.txtFollow, isAskingToFollow);
        updateFollowStatusOnPostsForBlog(post.blogId, post.getBlogUrl(), isAskingToFollow);
    }

    private void showReblogStatus(ImageView imgBtnReblog, boolean isRebloggedByCurrentUser) {
        if (isRebloggedByCurrentUser != imgBtnReblog.isSelected()) {
            imgBtnReblog.setSelected(isRebloggedByCurrentUser);
        }
        if (isRebloggedByCurrentUser) {
            imgBtnReblog.setOnClickListener(null);
        }
    }

    /*
     * AsyncTask to load posts in the current tag
     */
    private boolean mIsTaskRunning = false;
    private class LoadPostsTask extends AsyncTask<Void, Void, Boolean> {
        ReaderPostList tmpPosts;
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
            final int numExisting;
            switch (getPostListType()) {
                case TAG_PREVIEW: case TAG_FOLLOWED:
                    tmpPosts = ReaderPostTable.getPostsWithTag(mCurrentTag, MAX_ROWS, EXCLUDE_TEXT_COLUMN);
                    numExisting = ReaderPostTable.getNumPostsWithTag(mCurrentTag);
                    break;
                case BLOG_PREVIEW:
                    tmpPosts = ReaderPostTable.getPostsInBlog(mCurrentBlogId, MAX_ROWS, EXCLUDE_TEXT_COLUMN);
                    numExisting = ReaderPostTable.getNumPostsInBlog(mCurrentBlogId);
                    break;
                default:
                    return false;
            }

            if (mPosts.isSameList(tmpPosts)) {
                return false;
            }

            // if we're not already displaying the max # posts, enable requesting more when
            // the user scrolls to the end of the list
            mCanRequestMorePosts = (numExisting < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY);

            // pre-calc avatar URLs, featured image URLs, display tag, and pubDates in each
            // post - these values are all cached by the post after the first time they're
            // computed, so calling these getters ensures the values are immediately available
            // when accessed from getView
            String currentTagName = (mCurrentTag != null ? mCurrentTag.getTagName() : "");
            for (ReaderPost post: tmpPosts) {
                post.getPostAvatarForDisplay(mAvatarSz);
                post.getFeaturedImageForDisplay(mPhotonWidth, mPhotonHeight);
                post.getDatePublished();
                post.getTagForDisplay(currentTagName);
            }

            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mPosts = (ReaderPostList)(tmpPosts.clone());
                notifyDataSetChanged();
            }

            if (mDataLoadedListener != null) {
                mDataLoadedListener.onDataLoaded(isEmpty());
            }

            mIsTaskRunning = false;
        }
    }
}

