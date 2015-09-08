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
import android.widget.LinearLayout;
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
import org.wordpress.android.ui.reader.views.ReaderBlogInfoView;
import org.wordpress.android.ui.reader.views.ReaderIconCountView;
import org.wordpress.android.ui.reader.views.ReaderTagInfoView;
import org.wordpress.android.ui.reader.views.ReaderTagToolbar;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class ReaderPostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ReaderTag mCurrentTag;
    private long mCurrentBlogId;
    private long mCurrentFeedId;

    private final int mPhotonWidth;
    private final int mPhotonHeight;
    private final int mAvatarSzMedium;
    private final int mAvatarSzSmall;
    private final int mMarginLarge;

    private final String mWordCountFmtStr;
    private final String mReadingTimeFmtStr;

    private boolean mCanRequestMorePosts;
    private final boolean mShowTagToolbar;
    private final boolean mIsLoggedOutReader;

    private final ReaderTypes.ReaderPostListType mPostListType;
    private final ReaderPostList mPosts = new ReaderPostList();

    private ReaderInterfaces.OnPostSelectedListener mPostSelectedListener;
    private ReaderInterfaces.OnTagSelectedListener mOnTagSelectedListener;
    private ReaderTagToolbar.OnTagChangedListener mOnTagChangedListener;
    private ReaderInterfaces.OnPostPopupListener mOnPostPopupListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;
    private ReaderActions.DataRequestedListener mDataRequestedListener;
    private ReaderBlogInfoView.OnBlogInfoLoadedListener mBlogInfoLoadedListener;

    // the large "tbl_posts.text" column is unused here, so skip it when querying
    private static final boolean EXCLUDE_TEXT_COLUMN = true;
    private static final int MAX_ROWS = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY;

    // Longreads says that people can read 250 words per minute
    private static final int READING_WORDS_PER_MINUTE = 250;
    private static final int MIN_READING_TIME_MINUTES = 2;

    private static final int VIEW_TYPE_POST        = 0;
    private static final int VIEW_TYPE_BLOG_INFO   = 1;
    private static final int VIEW_TYPE_TAG_INFO    = 2;
    private static final int VIEW_TYPE_TAG_TOOLBAR = 3;

    private static final long ITEM_ID_CUSTOM_VIEW = -1L;

    class ReaderPostViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;

        private final TextView txtTitle;
        private final TextView txtText;
        private final TextView txtBlogName;
        private final TextView txtDate;
        private final TextView txtTag;
        private final TextView txtWordCount;

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
            txtWordCount = (TextView) itemView.findViewById(R.id.text_word_count);

            commentCount = (ReaderIconCountView) itemView.findViewById(R.id.count_comments);
            likeCount = (ReaderIconCountView) itemView.findViewById(R.id.count_likes);

            imgFeatured = (WPNetworkImageView) itemView.findViewById(R.id.image_featured);
            imgAvatar = (WPNetworkImageView) itemView.findViewById(R.id.image_avatar);
            imgMore = (ImageView) itemView.findViewById(R.id.image_more);

            layoutDiscover = (ViewGroup) itemView.findViewById(R.id.layout_discover);
            imgDiscoverAvatar = (WPNetworkImageView) layoutDiscover.findViewById(R.id.image_discover_avatar);
            txtDiscover = (TextView) layoutDiscover.findViewById(R.id.text_discover);

            layoutPostHeader = (ViewGroup) itemView.findViewById(R.id.layout_post_header);

            // adjust the right padding of the post header to allow right padding of the  "..." icon
            // https://github.com/wordpress-mobile/WordPress-Android/issues/3078
            layoutPostHeader.setPadding(
                    layoutPostHeader.getPaddingLeft(),
                    layoutPostHeader.getPaddingTop(),
                    layoutPostHeader.getPaddingRight() - imgMore.getPaddingRight(),
                    layoutPostHeader.getPaddingBottom());

            ReaderUtils.setBackgroundToRoundRipple(imgMore);
        }
    }

    class TagToolbarViewHolder extends RecyclerView.ViewHolder {
        private final ReaderTagToolbar mTagToolbar;
        public TagToolbarViewHolder(View itemView) {
            super(itemView);
            mTagToolbar = (ReaderTagToolbar) itemView;
        }
    }

    class BlogInfoViewHolder extends RecyclerView.ViewHolder {
        private final ReaderBlogInfoView mBlogInfoView;
        public BlogInfoViewHolder(View itemView) {
            super(itemView);
            mBlogInfoView = (ReaderBlogInfoView) itemView;
        }
    }

    class TagInfoViewHolder extends RecyclerView.ViewHolder {
        private final ReaderTagInfoView mTagInfoView;
        public TagInfoViewHolder(View itemView) {
            super(itemView);
            mTagInfoView = (ReaderTagInfoView) itemView;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && mShowTagToolbar) {
            // first item is a pseudo-toolbar enabling changing the current tag
            return VIEW_TYPE_TAG_TOOLBAR;
        } else if (position == 0 && isBlogPreview()) {
            // first item is a ReaderBlogInfoView
            return VIEW_TYPE_BLOG_INFO;
        } else if (position == 0 && isTagPreview()) {
            // first item is a ReaderTagInfoView
            return VIEW_TYPE_TAG_INFO;
        }
        return VIEW_TYPE_POST;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        switch (viewType) {
            case VIEW_TYPE_TAG_TOOLBAR:
                return new TagToolbarViewHolder(new ReaderTagToolbar(context));

            case VIEW_TYPE_BLOG_INFO:
                return new BlogInfoViewHolder(new ReaderBlogInfoView(context));

            case VIEW_TYPE_TAG_INFO:
                return new TagInfoViewHolder(new ReaderTagInfoView(context));

            default:
                View postView = LayoutInflater.from(context).inflate(R.layout.reader_cardview_post, parent, false);
                return new ReaderPostViewHolder(postView);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (!(holder instanceof ReaderPostViewHolder)) {
            if (holder instanceof BlogInfoViewHolder) {
                BlogInfoViewHolder blogHolder = (BlogInfoViewHolder) holder;
                blogHolder.mBlogInfoView.setOnBlogInfoLoadedListener(mBlogInfoLoadedListener);
                blogHolder.mBlogInfoView.loadBlogInfo(mCurrentBlogId, mCurrentFeedId);
            } else if (holder instanceof TagInfoViewHolder) {
                TagInfoViewHolder tagHolder = (TagInfoViewHolder) holder;
                tagHolder.mTagInfoView.setCurrentTag(mCurrentTag);
            } else if (holder instanceof TagToolbarViewHolder) {
                TagToolbarViewHolder toolbarHolder = (TagToolbarViewHolder) holder;
                toolbarHolder.mTagToolbar.setCurrentTag(mCurrentTag);
                toolbarHolder.mTagToolbar.setOnTagChangedListener(mOnTagChangedListener);
            }
            return;
        }

        final ReaderPost post = getItem(position);
        final ReaderPostViewHolder postHolder = (ReaderPostViewHolder) holder;
        ReaderTypes.ReaderPostListType postListType = getPostListType();

        postHolder.txtTitle.setText(post.getTitle());

        // dateline includes author name if different than blog name
        String dateLine;
        if (post.hasAuthorName() && !post.getAuthorName().equalsIgnoreCase(post.getBlogName())) {
            dateLine = post.getAuthorName() + " \u2022 " + DateTimeUtils.javaDateToTimeSpan(post.getDatePublished());
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

        // show blog preview when post header is tapped unless this already is blog preview
        if (!isBlogPreview()) {
            postHolder.layoutPostHeader.setOnClickListener(new View.OnClickListener() {
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
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) postHolder.txtTitle.getLayoutParams();
        params.topMargin = titleMargin;

        // show word count when appropriate, include reading time if at least two minutes
        if (post.wordCount > 0 && !post.isDiscoverPost()) {
            String wordCountStr = String.format(mWordCountFmtStr, post.wordCount);
            int readingTimeInMinutes = post.wordCount / READING_WORDS_PER_MINUTE;
            if (readingTimeInMinutes >= MIN_READING_TIME_MINUTES) {
                wordCountStr += " (~" + String.format(mReadingTimeFmtStr, readingTimeInMinutes) + ")";
            }
            postHolder.txtWordCount.setText(wordCountStr);
            postHolder.txtWordCount.setVisibility(View.VISIBLE);
        } else {
            postHolder.txtWordCount.setVisibility(View.GONE);
        }

        // show the best tag for this post
        final String tagToDisplay = (mCurrentTag != null ? post.getTagForDisplay(mCurrentTag.getTagName()) : null);
        if (!TextUtils.isEmpty(tagToDisplay)) {
            postHolder.txtTag.setText(ReaderUtils.makeHashTag(tagToDisplay));
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
            showComments = post.isWP() && !post.isJetpack && (post.isCommentsOpen || post.numReplies > 0);
        }

        if (showLikes || showComments) {
            showCounts(postHolder, post);
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
                                    discoverData.getBlogId());
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

        mWordCountFmtStr = context.getString(R.string.reader_label_word_count);
        mReadingTimeFmtStr = context.getString(R.string.reader_label_reading_time_in_minutes);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int cardMargin = context.getResources().getDimensionPixelSize(R.dimen.reader_card_margin);
        mPhotonWidth = displayWidth - (cardMargin * 2);
        mPhotonHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height);

        mShowTagToolbar = (getPostListType() == ReaderTypes.ReaderPostListType.TAG_FOLLOWED);

        setHasStableIds(true);
    }

    private boolean hasCustomFirstItem() {
        return mShowTagToolbar || isBlogPreview() || isTagPreview();
    }

    private boolean isBlogPreview() {
        return getPostListType() == ReaderTypes.ReaderPostListType.BLOG_PREVIEW;
    }

    private boolean isTagPreview() {
        return getPostListType() == ReaderTypes.ReaderPostListType.TAG_PREVIEW;
    }

    public void setOnPostSelectedListener(ReaderInterfaces.OnPostSelectedListener listener) {
        mPostSelectedListener = listener;
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

    public void setOnBlogInfoLoadedListener(ReaderBlogInfoView.OnBlogInfoLoadedListener listener) {
        mBlogInfoLoadedListener = listener;
    }

    /*
     * called when user clicks a tag
     */
    public void setOnTagSelectedListener(ReaderInterfaces.OnTagSelectedListener listener) {
        mOnTagSelectedListener = listener;
    }

    /*
     * called when user selects a tag from the toolbar
     */
    public void setOnTagChangedListener(ReaderTagToolbar.OnTagChangedListener listener) {
        mOnTagChangedListener = listener;
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
    public void setCurrentBlogAndFeed(long blogId, long feedId) {
        if (blogId != mCurrentBlogId || feedId != mCurrentFeedId) {
            mCurrentBlogId = blogId;
            mCurrentFeedId = feedId;
            reload();
        }
    }

    private void clear() {
        mPosts.clear();
        notifyDataSetChanged();
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
        if (hasCustomFirstItem()) {
            return position == 0 ? null : mPosts.get(position - 1);
        }
        return mPosts.get(position);
    }

    @Override
    public int getItemCount() {
        if (hasCustomFirstItem()) {
            return mPosts.size() + 1;
        }
        return mPosts.size();
    }

    public boolean isEmpty() {
        return (mPosts == null || mPosts.size() == 0);
    }

    @Override
    public long getItemId(int position) {
        if (getItemViewType(position) == VIEW_TYPE_POST) {
            return getItem(position).getStableId();
        } else {
            return ITEM_ID_CUSTOM_VIEW;
        }
    }

    /*
     * shows like & comment count
     */
    private void showCounts(ReaderPostViewHolder holder, ReaderPost post) {
        holder.likeCount.setCount(post.numLikes);

        if (post.numReplies > 0 || post.isCommentsOpen) {
            holder.commentCount.setCount(post.numReplies);
            holder.commentCount.setVisibility(View.VISIBLE);
        } else {
            holder.commentCount.setVisibility(View.GONE);
        }
    }

    /*
     * triggered when user taps the like button (textView)
     */
    private void toggleLike(Context context, ReaderPostViewHolder holder, ReaderPost post) {
        if (post == null || !NetworkUtils.checkConnection(context)) {
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
            showCounts(holder, updatedPost);
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
                    if (mCurrentFeedId != 0) {
                        allPosts = ReaderPostTable.getPostsInFeed(mCurrentFeedId, MAX_ROWS, EXCLUDE_TEXT_COLUMN);
                        numExisting = ReaderPostTable.getNumPostsInFeed(mCurrentFeedId);
                    } else {
                        allPosts = ReaderPostTable.getPostsInBlog(mCurrentBlogId, MAX_ROWS, EXCLUDE_TEXT_COLUMN);
                        numExisting = ReaderPostTable.getNumPostsInBlog(mCurrentBlogId);
                    }
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

