package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
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
import org.wordpress.android.models.ReaderPostDiscoverData;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderAnim;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.ReaderInterfaces;
import org.wordpress.android.ui.reader.ReaderTypes;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.views.ReaderIconCountView;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class ReaderPostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ReaderTag mCurrentTag;
    private long mCurrentBlogId;

    private final int mPhotonWidth;
    private final int mPhotonHeight;
    private final int mAvatarSzMedium;
    private final int mAvatarSzSmall;
    private final int mMarginLarge;

    private boolean mCanRequestMorePosts;
    private boolean mShowToolbarSpacer;
    private final boolean mIsLoggedOutReader;

    private final ReaderTypes.ReaderPostListType mPostListType;
    private final ReaderPostList mPosts = new ReaderPostList();

    private ReaderInterfaces.OnPostSelectedListener mPostSelectedListener;
    private ReaderInterfaces.OnTagSelectedListener mOnTagSelectedListener;
    private ReaderInterfaces.OnPostPopupListener mOnPostPopupListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;
    private ReaderActions.DataRequestedListener mDataRequestedListener;

    // the large "tbl_posts.text" column is unused here, so skip it when querying
    private static final boolean EXCLUDE_TEXT_COLUMN = true;
    private static final int MAX_ROWS = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY;

    private static final int VIEW_TYPE_SPACER = 1;
    private static final int VIEW_TYPE_POST = 2;
    private static final long ITEM_ID_SPACER = -1L;

    class ReaderPostViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;

        private final TextView txtTitle;
        private final TextView txtText;
        private final TextView txtBlogName;
        private final TextView txtDate;
        private final TextView txtTag;

        private final ReaderIconCountView commentCount;
        private final ReaderIconCountView likeCount;

        private final ImageView imgMore;

        private final WPNetworkImageView imgFeatured;
        private final WPNetworkImageView imgAvatar;

        private final ViewGroup layoutPostHeader;

        private final ViewGroup layoutDiscover;
        private final WPNetworkImageView imgDiscoverAvatar;
        private final TextView txtDiscover;

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

            imgFeatured = (WPNetworkImageView) itemView.findViewById(R.id.image_featured);
            imgAvatar = (WPNetworkImageView) itemView.findViewById(R.id.image_avatar);
            imgMore = (ImageView) itemView.findViewById(R.id.image_more);

            layoutPostHeader = (ViewGroup) itemView.findViewById(R.id.layout_post_header);

            layoutDiscover = (ViewGroup) itemView.findViewById(R.id.layout_discover);
            imgDiscoverAvatar = (WPNetworkImageView) layoutDiscover.findViewById(R.id.image_discover_avatar);
            txtDiscover = (TextView) layoutDiscover.findViewById(R.id.text_discover);

            // blog name isn't tappable when showing blog preview, so change to disabled color
            if (getPostListType() == ReaderTypes.ReaderPostListType.BLOG_PREVIEW) {
                int color = itemView.getContext().getResources().getColor(R.color.grey_lighten_10);
                txtBlogName.setTextColor(color);
            }
        }
    }

    class SpacerViewHolder extends RecyclerView.ViewHolder {
        public SpacerViewHolder(View itemView) {
            super(itemView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && mShowToolbarSpacer) {
            return VIEW_TYPE_SPACER;
        }
        return VIEW_TYPE_POST;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        if (viewType == VIEW_TYPE_SPACER) {
            View spacerView = new View(context);
            int toolbarHeight = context.getResources().getDimensionPixelSize(R.dimen.toolbar_height);
            int dividerHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_card_gutters);
            int spacerHeight = toolbarHeight - dividerHeight;
            spacerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, spacerHeight));
            return new SpacerViewHolder(spacerView);
        } else {
            View postView = LayoutInflater.from(context).inflate(R.layout.reader_cardview_post, parent, false);
            return new ReaderPostViewHolder(postView);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof SpacerViewHolder) {
            return;
        }

        final ReaderPost post = getItem(position);
        final ReaderPostViewHolder postHolder = (ReaderPostViewHolder) holder;
        ReaderTypes.ReaderPostListType postListType = getPostListType();

        postHolder.txtTitle.setText(post.getTitle());

        // dateline includes author name if different than blog name
        String dateLine;
        if (post.hasAuthorName() && !post.getAuthorName().equalsIgnoreCase(post.getBlogName())) {
            dateLine = post.getAuthorName() + ", " + DateTimeUtils.javaDateToTimeSpan(post.getDatePublished());
        } else {
            dateLine = DateTimeUtils.javaDateToTimeSpan(post.getDatePublished());
        }
        postHolder.txtDate.setText(dateLine);

        if (post.hasBlogUrl()) {
            String imageUrl = GravatarUtils.blavatarFromUrl(post.getUrl(), mAvatarSzMedium);
            postHolder.imgAvatar.setImageUrl(imageUrl, WPNetworkImageView.ImageType.BLAVATAR);
        } else {
            postHolder.imgAvatar.setImageUrl(post.getPostAvatarForDisplay(mAvatarSzMedium), WPNetworkImageView.ImageType.AVATAR);
        }
        if (post.hasBlogName()) {
            postHolder.txtBlogName.setText(post.getBlogName());
        } else if (post.hasAuthorName()) {
            postHolder.txtBlogName.setText(post.getAuthorName());
        } else {
            postHolder.txtBlogName.setText(null);
        }

        // if we're not showing blog preview, show blog/feed preview when avatar or blog name is tapped
        if (getPostListType() != ReaderTypes.ReaderPostListType.BLOG_PREVIEW) {
            View.OnClickListener blogListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReaderActivityLauncher.showReaderBlogPreview(view.getContext(), post);
                }
            };
            postHolder.imgAvatar.setOnClickListener(blogListener);
            postHolder.txtBlogName.setOnClickListener(blogListener);
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
            if (tagToDisplay.startsWith("#")) {
                postHolder.txtTag.setText(tagToDisplay);
            } else {
                postHolder.txtTag.setText("#" + tagToDisplay);
            }
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

        boolean showLikes;
        boolean showComments;
        if (post.isDiscoverPost()) {
            showLikes = false;
            showComments = false;
        } else if (mIsLoggedOutReader) {
            showLikes = post.numLikes > 0;
            showComments = post.numReplies > 0;
        } else {
            showLikes = post.isWP() && post.isLikesEnabled;
            showComments = post.isWP() && (post.isCommentsOpen || post.numReplies > 0);
        }

        if (showLikes || showComments) {
            showCounts(postHolder, post, false);
        }

        if (showLikes) {
            postHolder.likeCount.setSelected(post.isLikedByCurrentUser);
            postHolder.likeCount.setVisibility(View.VISIBLE);
            // can't like when logged out
            if (mIsLoggedOutReader) {
                postHolder.likeCount.setEnabled(false);
                postHolder.likeCount.setOnClickListener(null);
            } else {
                postHolder.likeCount.setEnabled(true);
                postHolder.likeCount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleLike(v.getContext(), postHolder, post);
                    }
                });
            }
        } else {
            postHolder.likeCount.setVisibility(View.GONE);
            postHolder.likeCount.setOnClickListener(null);
        }

        if (showComments) {
            postHolder.commentCount.setVisibility(View.VISIBLE);
            postHolder.commentCount.setEnabled(true);
            postHolder.commentCount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.showReaderComments(v.getContext(), post.blogId, post.postId);
                }
            });
        } else {
            postHolder.commentCount.setVisibility(View.GONE);
            postHolder.commentCount.setOnClickListener(null);
        }

        // more menu only shows for followed tags
        if (!mIsLoggedOutReader && postListType == ReaderTypes.ReaderPostListType.TAG_FOLLOWED) {
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

        // attribution section for discover posts
        if (post.isDiscoverPost()) {
            showDiscoverData(postHolder, post);
        } else {
            postHolder.layoutDiscover.setVisibility(View.GONE);
        }

        // if we're nearing the end of the posts, fire request to load more
        if (mCanRequestMorePosts && mDataRequestedListener != null && (position >= getItemCount() - 1)) {
            mDataRequestedListener.onRequestData();
        }

        if (mPostSelectedListener != null) {
            postHolder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPostSelectedListener.onPostSelected(post);
                }
            });
        }
    }

    private void showDiscoverData(final ReaderPostViewHolder postHolder,
                                  final ReaderPost post) {
        final ReaderPostDiscoverData discoverData = post.getDiscoverData();
        if (discoverData == null) {
            postHolder.layoutDiscover.setVisibility(View.GONE);
            return;
        }

        postHolder.layoutDiscover.setVisibility(View.VISIBLE);
        postHolder.txtDiscover.setText(discoverData.getAttributionHtml());

        switch (discoverData.getDiscoverType()) {
            case EDITOR_PICK:
                if (discoverData.hasAvatarUrl()) {
                    postHolder.imgDiscoverAvatar.setImageUrl(GravatarUtils.fixGravatarUrl(discoverData.getAvatarUrl(), mAvatarSzSmall), WPNetworkImageView.ImageType.AVATAR);
                } else {
                    postHolder.imgDiscoverAvatar.showDefaultGravatarImage();
                }
                // tapping an editor pick opens the source post, which is handled by the existing
                // post selection handler
                postHolder.layoutDiscover.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mPostSelectedListener != null) {
                            mPostSelectedListener.onPostSelected(post);
                        }
                    }
                });
                break;

            case SITE_PICK:
                if (discoverData.hasAvatarUrl()) {
                    postHolder.imgDiscoverAvatar.setImageUrl(
                            GravatarUtils.fixGravatarUrl(discoverData.getAvatarUrl(), mAvatarSzSmall), WPNetworkImageView.ImageType.BLAVATAR);
                } else {
                    postHolder.imgDiscoverAvatar.showDefaultBlavatarImage();
                }
                // site picks show "Visit [BlogName]" link - tapping opens the blog preview if
                // we have the blogId, if not show blog in internal webView
                postHolder.layoutDiscover.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (discoverData.getBlogId() != 0) {
                            ReaderActivityLauncher.showReaderBlogPreview(
                                    v.getContext(),
                                    discoverData.getBlogId(),
                                    discoverData.getBlogName());
                        } else if (discoverData.hasBlogUrl()) {
                            ReaderActivityLauncher.openUrl(v.getContext(), discoverData.getBlogUrl());
                        }
                    }
                });
                break;

            default:
                // something else, so hide discover section
                postHolder.layoutDiscover.setVisibility(View.GONE);
                break;
        }
    }

    // ********************************************************************************************

    public ReaderPostAdapter(Context context, ReaderTypes.ReaderPostListType postListType) {
        super();

        mPostListType = postListType;
        mAvatarSzMedium = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
        mAvatarSzSmall = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
        mMarginLarge = context.getResources().getDimensionPixelSize(R.dimen.margin_large);
        mIsLoggedOutReader = ReaderUtils.isLoggedOutReader();

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

    public void removePostsInBlog(long blogId) {
        int numRemoved = 0;
        ReaderPostList postsInBlog = mPosts.getPostsInBlog(blogId);
        for (ReaderPost post : postsInBlog) {
            int index = mPosts.indexOfPost(post);
            if (index > -1) {
                numRemoved++;
                mPosts.remove(index);
            }
        }
        if (numRemoved > 0) {
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

    private ReaderPost getItem(int position) {
        if (mShowToolbarSpacer) {
            return position == 0 ? null : mPosts.get(position - 1);
        } else {
            return mPosts.get(position);
        }
    }

    @Override
    public int getItemCount() {
        if (mShowToolbarSpacer && mPosts.size() > 0) {
            return mPosts.size() + 1;
        }
        return mPosts.size();
    }

    public boolean isEmpty() {
        return (mPosts == null || mPosts.size() == 0);
    }

    @Override
    public long getItemId(int position) {
        if (getItemViewType(position) == VIEW_TYPE_SPACER) {
            return ITEM_ID_SPACER;
        }
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
    private void toggleLike(Context context, ReaderPostViewHolder holder, ReaderPost post) {
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
        int position = mPosts.indexOfPost(post);
        ReaderPost updatedPost = ReaderPostTable.getPost(post.blogId, post.postId, true);
        if (updatedPost != null && position > -1) {
            mPosts.set(position, updatedPost);
            holder.likeCount.setSelected(updatedPost.isLikedByCurrentUser);
            showCounts(holder, updatedPost, true);
        }
    }

    public void setFollowStatusForBlog(long blogId, boolean isFollowing) {
        ReaderPost post;
        for (int i = 0; i < mPosts.size(); i++) {
            post = mPosts.get(i);
            if (post.blogId == blogId && post.isFollowedByCurrentUser != isFollowing) {
                post.isFollowedByCurrentUser = isFollowing;
                mPosts.set(i, post);
            }
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
            int numExisting;
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

