package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.widget.CardView;
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
import org.wordpress.android.ui.reader.views.ReaderFollowButton;
import org.wordpress.android.ui.reader.views.ReaderIconCountView;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class ReaderPostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ReaderTag mCurrentTag;
    private long mCurrentBlogId;

    private final int mPhotonWidth;
    private final int mPhotonHeight;
    private final int mAvatarSz;
    private final int mMarginLarge;

    private boolean mCanRequestMorePosts;
    private boolean mShowToolbarSpacer;

    private final ReaderTypes.ReaderPostListType mPostListType;
    private final ReaderPostList mPosts = new ReaderPostList();

    private ReaderInterfaces.OnPostSelectedListener mPostSelectedListener;
    private ReaderInterfaces.OnTagSelectedListener mOnTagSelectedListener;
    private ReaderInterfaces.OnPostPopupListener mOnPostPopupListener;
    private ReaderInterfaces.RequestReblogListener mReblogListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;
    private ReaderActions.DataRequestedListener mDataRequestedListener;

    // the large "tbl_posts.text" column is unused here, so skip it when querying
    private static final boolean EXCLUDE_TEXT_COLUMN = true;
    private static final int MAX_ROWS = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY;

    class ReaderPostViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;

        private final TextView txtTitle;
        private final TextView txtText;
        private final TextView txtBlogName;
        private final TextView txtDate;
        private final TextView txtTag;

        private final ReaderIconCountView commentCount;
        private final ReaderIconCountView likeCount;
        private final ReaderFollowButton followButton;

        private final ImageView imgBtnReblog;
        private final ImageView imgMore;

        private final WPNetworkImageView imgFeatured;
        private final WPNetworkImageView imgAvatar;

        private final ViewGroup layoutPostHeader;
        private final View toolbarSpacer;

        public ReaderPostViewHolder(View itemView) {
            super(itemView);

            cardView = (CardView) itemView.findViewById(R.id.card_view);

            txtTitle = (TextView) itemView.findViewById(R.id.text_title);
            txtText = (TextView) itemView.findViewById(R.id.text_excerpt);
            txtBlogName = (TextView) itemView.findViewById(R.id.text_blog_name);
            txtDate = (TextView) itemView.findViewById(R.id.text_date);
            txtTag = (TextView) itemView.findViewById(R.id.text_tag);

            commentCount = (ReaderIconCountView) itemView.findViewById(R.id.count_comments);
            likeCount = (ReaderIconCountView) itemView.findViewById(R.id.count_likes);
            followButton = (ReaderFollowButton) itemView.findViewById(R.id.follow_button);

            imgFeatured = (WPNetworkImageView) itemView.findViewById(R.id.image_featured);
            imgAvatar = (WPNetworkImageView) itemView.findViewById(R.id.image_avatar);
            imgMore = (ImageView) itemView.findViewById(R.id.image_more);

            layoutPostHeader = (ViewGroup) itemView.findViewById(R.id.layout_post_header);
            toolbarSpacer = itemView.findViewById(R.id.spacer_toolbar);

            imgBtnReblog = (ImageView) itemView.findViewById(R.id.image_reblog_btn);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                imgBtnReblog.setBackgroundResource(R.drawable.ripple_oval);
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View postView = LayoutInflater.from(parent.getContext()).inflate(R.layout.reader_cardview_post, parent, false);
        return new ReaderPostViewHolder(postView);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final ReaderPost post = getItem(position);
        final ReaderPostViewHolder postHolder = (ReaderPostViewHolder) holder;
        ReaderTypes.ReaderPostListType postListType = getPostListType();

        postHolder.txtTitle.setText(post.getTitle());
        postHolder.txtDate.setText(DateTimeUtils.javaDateToTimeSpan(post.getDatePublished()));

        postHolder.toolbarSpacer.setVisibility(position == 0 && mShowToolbarSpacer ? View.VISIBLE : View.GONE);

        // hide the post header (avatar, blog name & follow button) if we're showing posts
        // in a specific blog
        if (postListType == ReaderTypes.ReaderPostListType.BLOG_PREVIEW) {
            postHolder.layoutPostHeader.setVisibility(View.GONE);
        } else {
            postHolder.layoutPostHeader.setVisibility(View.VISIBLE);
            postHolder.imgAvatar.setImageUrl(post.getPostAvatarForDisplay(mAvatarSz), WPNetworkImageView.ImageType.AVATAR);
            if (post.hasBlogName()) {
                postHolder.txtBlogName.setText(post.getBlogName());
            } else if (post.hasAuthorName()) {
                postHolder.txtBlogName.setText(post.getAuthorName());
            } else {
                postHolder.txtBlogName.setText(null);
            }

            // follow/following
            postHolder.followButton.setIsFollowed(post.isFollowedByCurrentUser);
            postHolder.followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollow((ReaderFollowButton) v, position);
                }
            });

            // show blog/feed preview when avatar is tapped
            postHolder.imgAvatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReaderActivityLauncher.showReaderBlogPreview(view.getContext(), post);
                }
            });
        }

        if (post.hasExcerpt()) {
            postHolder.txtText.setVisibility(View.VISIBLE);
            postHolder.txtText.setText(post.getExcerpt());
        } else {
            postHolder.txtText.setVisibility(View.GONE);
        }

        final int titleMargin;
        if (post.hasFeaturedImage()) {
            final String imageUrl = post.getFeaturedImageForDisplay(mPhotonWidth, mPhotonHeight);
            postHolder.imgFeatured.setImageUrl(imageUrl, WPNetworkImageView.ImageType.PHOTO);
            postHolder.imgFeatured.setVisibility(View.VISIBLE);
            titleMargin = mMarginLarge;
        } else if (post.hasFeaturedVideo()) {
            postHolder.imgFeatured.setVideoUrl(post.postId, post.getFeaturedVideo());
            postHolder.imgFeatured.setVisibility(View.VISIBLE);
            titleMargin = mMarginLarge;
        } else {
            postHolder.imgFeatured.setVisibility(View.GONE);
            titleMargin = (postHolder.layoutPostHeader.getVisibility() == View.VISIBLE ? 0 : mMarginLarge);
        }

        // set the top margin of the title based on whether there's a featured image and post header
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) postHolder.txtTitle.getLayoutParams();
        params.topMargin = titleMargin;

        // show the best tag for this post
        final String tagToDisplay = (mCurrentTag != null ? post.getTagForDisplay(mCurrentTag.getTagName()) : null);
        if (!TextUtils.isEmpty(tagToDisplay)) {
            postHolder.txtTag.setText(tagToDisplay);
            postHolder.txtTag.setVisibility(View.VISIBLE);
            postHolder.txtTag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnTagSelectedListener != null) {
                        mOnTagSelectedListener.onTagSelected(tagToDisplay);
                    }
                }
            });
        } else {
            postHolder.txtTag.setVisibility(View.GONE);
            postHolder.txtTag.setOnClickListener(null);
        }

        // likes, comments & reblogging - supported by wp posts only
        boolean showLikes = post.isWP() && post.isLikesEnabled;
        boolean showComments = post.isWP() && (post.isCommentsOpen || post.numReplies > 0);

        if (showLikes || showComments) {
            showCounts(postHolder, post, false);
        }

        if (showLikes) {
            postHolder.likeCount.setSelected(post.isLikedByCurrentUser);
            postHolder.likeCount.setVisibility(View.VISIBLE);
            postHolder.likeCount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleLike(v.getContext(), postHolder, position);
                }
            });
        } else {
            postHolder.likeCount.setVisibility(View.GONE);
            postHolder.likeCount.setOnClickListener(null);
        }

        if (showComments) {
            postHolder.commentCount.setVisibility(View.VISIBLE);
            postHolder.commentCount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.showReaderComments(v.getContext(), post);
                }
            });
        } else {
            postHolder.commentCount.setVisibility(View.GONE);
            postHolder.commentCount.setOnClickListener(null);
        }

        if (post.canReblog()) {
            showReblogStatus(postHolder.imgBtnReblog, post.isRebloggedByCurrentUser);
            postHolder.imgBtnReblog.setVisibility(View.VISIBLE);
            if (!post.isRebloggedByCurrentUser) {
                postHolder.imgBtnReblog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ReaderAnim.animateReblogButton(postHolder.imgBtnReblog);
                        if (mReblogListener != null) {
                            mReblogListener.onRequestReblog(post, v);
                        }
                    }
                });
            } else {
                postHolder.imgBtnReblog.setOnClickListener(null);
            }
        } else {
            // use INVISIBLE rather than GONE to ensure container maintains the same height
            postHolder.imgBtnReblog.setVisibility(View.INVISIBLE);
            postHolder.imgBtnReblog.setOnClickListener(null);
        }

        // more menu with "block this blog" only shows for public wp posts in followed tags
        if (post.isWP() && !post.isPrivate && postListType == ReaderTypes.ReaderPostListType.TAG_FOLLOWED) {
            postHolder.imgMore.setVisibility(View.VISIBLE);
            postHolder.imgMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnPostPopupListener != null) {
                        mOnPostPopupListener.onShowPostPopup(view, post);
                    }
                }
            });
        } else {
            postHolder.imgMore.setVisibility(View.GONE);
            postHolder.imgMore.setOnClickListener(null);
        }

        // if we're nearing the end of the posts, fire request to load more
        if (mCanRequestMorePosts && mDataRequestedListener != null && (position >= getItemCount() - 1)) {
            mDataRequestedListener.onRequestData();
        }

        if (mPostSelectedListener != null) {
            postHolder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPostSelectedListener.onPostSelected(post.blogId, post.postId);
                }
            });
        }
    }

    // ********************************************************************************************

    public ReaderPostAdapter(Context context, ReaderTypes.ReaderPostListType postListType) {
        super();

        mPostListType = postListType;
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
        mMarginLarge = context.getResources().getDimensionPixelSize(R.dimen.margin_large);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int cardSpacing = context.getResources().getDimensionPixelSize(R.dimen.content_margin);
        mPhotonWidth = displayWidth - (cardSpacing * 2);
        mPhotonHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height);

        setHasStableIds(true);
    }

    /*
     * show spacer view above the first post to accommodate tag toolbar on ReaderPostListFragment
     */
    public void setShowToolbarSpacer(boolean show) {
        mShowToolbarSpacer = show;
    }

    public void setOnPostSelectedListener(ReaderInterfaces.OnPostSelectedListener listener) {
        mPostSelectedListener = listener;
    }

    public void setOnTagSelectedListener(ReaderInterfaces.OnTagSelectedListener listener) {
        mOnTagSelectedListener = listener;
    }

    public void setOnDataLoadedListener(ReaderInterfaces.DataLoadedListener listener) {
        mDataLoadedListener = listener;
    }

    public void setOnDataRequestedListener(ReaderActions.DataRequestedListener listener) {
        mDataRequestedListener = listener;
    }

    public void setOnReblogRequestedListener(ReaderInterfaces.RequestReblogListener listener) {
        mReblogListener = listener;
    }

    public void setOnPostPopupListener(ReaderInterfaces.OnPostPopupListener onPostPopupListener) {
        mOnPostPopupListener = onPostPopupListener;
    }

    private ReaderTypes.ReaderPostListType getPostListType() {
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
    private void reload() {
        clear();
        loadPosts();
    }

    private void removePost(ReaderPost post) {
        int index = mPosts.indexOfPost(post);
        if (index > -1) {
            mPosts.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void removePostsInBlog(long blogId) {
        ReaderPostList postsInBlog = mPosts.getPostsInBlog(blogId);
        for (ReaderPost post : postsInBlog) {
            removePost(post);
        }
    }

    /*
     * reload a single post
     */
    public void reloadPost(ReaderPost post) {
        int index = mPosts.indexOfPost(post);
        if (index == -1) {
            return;
        }

        final ReaderPost updatedPost = ReaderPostTable.getPost(post.blogId, post.postId, true);
        if (updatedPost != null) {
            mPosts.set(index, updatedPost);
            notifyItemChanged(index);
        }
    }

    /*
     * copy the follow status from the passed post to other posts in the same blog
     */
    private void copyBlogFollowStatus(final ReaderPost post) {
        if (isEmpty() || post == null) {
            return;
        }

        long blogId = post.blogId;
        String blogUrl = post.getBlogUrl();

        boolean hasBlogId = (blogId != 0);
        boolean hasBlogUrl = !TextUtils.isEmpty(blogUrl);
        if (!hasBlogId && !hasBlogUrl) {
            return;
        }

        long skipPostId = post.postId;
        boolean followStatus = post.isFollowedByCurrentUser;
        boolean isMatched;

        for (ReaderPost thisPost : mPosts) {
            if (hasBlogId) {
                isMatched = (blogId == thisPost.blogId && skipPostId != thisPost.postId);
            } else {
                isMatched = blogUrl.equals(thisPost.getBlogUrl());
            }
            if (isMatched && thisPost.isFollowedByCurrentUser != followStatus) {
                thisPost.isFollowedByCurrentUser = followStatus;
                int position = mPosts.indexOfPost(thisPost);
                if (position > -1) {
                    notifyItemChanged(position);
                }
            }
        }
    }

    private void loadPosts() {
        if (mIsTaskRunning) {
            AppLog.w(AppLog.T.READER, "reader posts task already running");
            return;
        }
        new LoadPostsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private ReaderPost getItem(int position) {
        return mPosts.get(position);
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    public boolean isEmpty() {
        return (mPosts == null || mPosts.size() == 0);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getStableId();
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
    private void toggleLike(Context context, ReaderPostViewHolder holder, int position) {
        ReaderPost post = getItem(position);
        if (post == null) {
            return;
        }

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
        if (updatedPost != null) {
            mPosts.set(position, updatedPost);
            holder.likeCount.setSelected(updatedPost.isLikedByCurrentUser);
            showCounts(holder, updatedPost, true);
        }
    }

    /*
     * triggered when user taps the follow button
     */
    private void toggleFollow(final ReaderFollowButton followButton, int position) {
        ReaderPost post = getItem(position);
        if (post == null) {
            return;
        }

        final boolean isAskingToFollow = !post.isFollowedByCurrentUser;
        followButton.setIsFollowedAnimated(isAskingToFollow);

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!succeeded) {
                    int resId = (isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog);
                    ToastUtils.showToast(followButton.getContext(), resId);
                    followButton.setIsFollowed(!isAskingToFollow);
                }
            }
        };

        if (ReaderBlogActions.followBlogForPost(post, isAskingToFollow, actionListener)) {
            ReaderPost updatedPost = ReaderPostTable.getPost(post.blogId, post.postId, true);
            if (updatedPost != null) {
                mPosts.set(position, updatedPost);
                copyBlogFollowStatus(updatedPost);
            }
        }
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
        ReaderPostList allPosts;

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
                case TAG_PREVIEW:
                case TAG_FOLLOWED:
                    allPosts = ReaderPostTable.getPostsWithTag(mCurrentTag, MAX_ROWS, EXCLUDE_TEXT_COLUMN);
                    numExisting = ReaderPostTable.getNumPostsWithTag(mCurrentTag);
                    break;
                case BLOG_PREVIEW:
                    allPosts = ReaderPostTable.getPostsInBlog(mCurrentBlogId, MAX_ROWS, EXCLUDE_TEXT_COLUMN);
                    numExisting = ReaderPostTable.getNumPostsInBlog(mCurrentBlogId);
                    break;
                default:
                    return false;
            }

            if (mPosts.isSameList(allPosts)) {
                return false;
            }

            // if we're not already displaying the max # posts, enable requesting more when
            // the user scrolls to the end of the list
            mCanRequestMorePosts = (numExisting < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mPosts.clear();
                mPosts.addAll(allPosts);
                notifyDataSetChanged();
            }

            if (mDataLoadedListener != null) {
                mDataLoadedListener.onDataLoaded(isEmpty());
            }

            mIsTaskRunning = false;
        }
    }
}

