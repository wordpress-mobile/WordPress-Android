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
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.utils.ReaderXPostUtils;
import org.wordpress.android.ui.reader.views.ReaderBlogInfoView;
import org.wordpress.android.ui.reader.views.ReaderGapMarkerView;
import org.wordpress.android.ui.reader.views.ReaderIconCountView;
import org.wordpress.android.ui.reader.views.ReaderTagInfoView;
import org.wordpress.android.ui.reader.views.ReaderThumbnailStrip;
import org.wordpress.android.util.AnalyticsUtils;
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
    private int mGapMarkerPosition = -1;

    private final int mPhotonWidth;
    private final int mPhotonHeight;
    private final int mAvatarSzMedium;
    private final int mAvatarSzSmall;
    private final int mMarginLarge;

    private final String mWordCountFmtStr;
    private final String mReadingTimeFmtStr;

    private boolean mCanRequestMorePosts;
    private final boolean mIsLoggedOutReader;

    private final ReaderTypes.ReaderPostListType mPostListType;
    private final ReaderPostList mPosts = new ReaderPostList();

    private ReaderInterfaces.OnPostSelectedListener mPostSelectedListener;
    private ReaderInterfaces.OnTagSelectedListener mOnTagSelectedListener;
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
    private static final int VIEW_TYPE_XPOST       = 1;
    private static final int VIEW_TYPE_BLOG_INFO   = 2;
    private static final int VIEW_TYPE_TAG_INFO    = 3;
    private static final int VIEW_TYPE_GAP_MARKER  = 4;

    private static final long ITEM_ID_CUSTOM_VIEW = -1L;

    /*
     * cross-post
     */
    class ReaderXPostViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final WPNetworkImageView imgAvatar;
        private final WPNetworkImageView imgBlavatar;
        private final TextView txtTitle;
        private final TextView txtSubtitle;

        public ReaderXPostViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.card_view);
            imgAvatar = (WPNetworkImageView) itemView.findViewById(R.id.image_avatar);
            imgBlavatar = (WPNetworkImageView) itemView.findViewById(R.id.image_blavatar);
            txtTitle = (TextView) itemView.findViewById(R.id.text_title);
            txtSubtitle = (TextView) itemView.findViewById(R.id.text_subtitle);
        }
    }

    /*
     * full post
     */
    class ReaderPostViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;

        private final TextView txtTitle;
        private final TextView txtText;
        private final TextView txtBlogName;
        private final TextView txtDate;
        private final TextView txtTag;
        private final TextView txtWordCount;
        private final TextView txtDateBelowTitle;

        private final ReaderIconCountView commentCount;
        private final ReaderIconCountView likeCount;

        private final ImageView imgMore;

        private final WPNetworkImageView imgFeatured;
        private final WPNetworkImageView imgAvatar;

        private final ViewGroup layoutPostHeader;

        private final ViewGroup layoutDiscover;
        private final WPNetworkImageView imgDiscoverAvatar;
        private final TextView txtDiscover;

        private final ReaderThumbnailStrip thumbnailStrip;

        public ReaderPostViewHolder(View itemView) {
            super(itemView);

            cardView = (CardView) itemView.findViewById(R.id.card_view);

            txtTitle = (TextView) itemView.findViewById(R.id.text_title);
            txtText = (TextView) itemView.findViewById(R.id.text_excerpt);
            txtBlogName = (TextView) itemView.findViewById(R.id.text_blog_name);
            txtDate = (TextView) itemView.findViewById(R.id.text_date);
            txtTag = (TextView) itemView.findViewById(R.id.text_tag);
            txtWordCount = (TextView) itemView.findViewById(R.id.text_word_count);
            txtDateBelowTitle = (TextView) itemView.findViewById(R.id.text_date_below_title);

            commentCount = (ReaderIconCountView) itemView.findViewById(R.id.count_comments);
            likeCount = (ReaderIconCountView) itemView.findViewById(R.id.count_likes);

            imgFeatured = (WPNetworkImageView) itemView.findViewById(R.id.image_featured);
            imgAvatar = (WPNetworkImageView) itemView.findViewById(R.id.image_avatar);
            imgMore = (ImageView) itemView.findViewById(R.id.image_more);

            layoutDiscover = (ViewGroup) itemView.findViewById(R.id.layout_discover);
            imgDiscoverAvatar = (WPNetworkImageView) layoutDiscover.findViewById(R.id.image_discover_avatar);
            txtDiscover = (TextView) layoutDiscover.findViewById(R.id.text_discover);

            thumbnailStrip = (ReaderThumbnailStrip) itemView.findViewById(R.id.thumbnail_strip);

            layoutPostHeader = (ViewGroup) itemView.findViewById(R.id.layout_post_header);

            // post header isn't shown for blog preview
            if (!isBlogPreview()) {
                // adjust the right padding of the post header to allow right padding of the  "..." icon
                // https://github.com/wordpress-mobile/WordPress-Android/issues/3078
                layoutPostHeader.setPadding(
                        layoutPostHeader.getPaddingLeft(),
                        layoutPostHeader.getPaddingTop(),
                        layoutPostHeader.getPaddingRight() - imgMore.getPaddingRight(),
                        layoutPostHeader.getPaddingBottom());
            } else {
                // hide the header
                layoutPostHeader.setVisibility(View.GONE);
                // add a bit more padding above the title
                int extraPadding = itemView.getContext().getResources().getDimensionPixelSize(R.dimen.margin_medium);
                txtTitle.setPadding(
                        txtTitle.getPaddingLeft(),
                        txtTitle.getTotalPaddingTop() + extraPadding,
                        txtTitle.getPaddingRight(),
                        txtTitle.getPaddingBottom());
                // show the dateline that appears below the title (hidden in layout)
                txtDateBelowTitle.setVisibility(View.VISIBLE);
            }

            ReaderUtils.setBackgroundToRoundRipple(imgMore);
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

    class GapMarkerViewHolder extends RecyclerView.ViewHolder {
        private final ReaderGapMarkerView mGapMarkerView;
        public GapMarkerViewHolder(View itemView) {
            super(itemView);
            mGapMarkerView = (ReaderGapMarkerView) itemView;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && isBlogPreview()) {
            // first item is a ReaderBlogInfoView
            return VIEW_TYPE_BLOG_INFO;
        } else if (position == 0 && isTagPreview()) {
            // first item is a ReaderTagInfoView
            return VIEW_TYPE_TAG_INFO;
        } else if (position == mGapMarkerPosition) {
            return VIEW_TYPE_GAP_MARKER;
        } else if (getItem(position).isXpost()) {
            return VIEW_TYPE_XPOST;
        } else {
            return VIEW_TYPE_POST;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        switch (viewType) {
            case VIEW_TYPE_BLOG_INFO:
                return new BlogInfoViewHolder(new ReaderBlogInfoView(context));

            case VIEW_TYPE_TAG_INFO:
                return new TagInfoViewHolder(new ReaderTagInfoView(context));

            case VIEW_TYPE_GAP_MARKER:
                return new GapMarkerViewHolder(new ReaderGapMarkerView(context));

            case VIEW_TYPE_XPOST:
                View xpostView = LayoutInflater.from(context).inflate(R.layout.reader_cardview_xpost, parent, false);
                return new ReaderXPostViewHolder(xpostView);

            default:
                View postView = LayoutInflater.from(context).inflate(R.layout.reader_cardview_post, parent, false);
                return new ReaderPostViewHolder(postView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ReaderPostViewHolder) {
            renderPost(position, (ReaderPostViewHolder) holder);
        } else if (holder instanceof ReaderXPostViewHolder) {
            renderXPost(position, (ReaderXPostViewHolder) holder);
        } else if (holder instanceof BlogInfoViewHolder) {
            BlogInfoViewHolder blogHolder = (BlogInfoViewHolder) holder;
            blogHolder.mBlogInfoView.setOnBlogInfoLoadedListener(mBlogInfoLoadedListener);
            blogHolder.mBlogInfoView.loadBlogInfo(mCurrentBlogId, mCurrentFeedId);
        } else if (holder instanceof TagInfoViewHolder) {
            TagInfoViewHolder tagHolder = (TagInfoViewHolder) holder;
            tagHolder.mTagInfoView.setCurrentTag(mCurrentTag);
        } else if (holder instanceof GapMarkerViewHolder) {
            GapMarkerViewHolder gapHolder = (GapMarkerViewHolder) holder;
            gapHolder.mGapMarkerView.setCurrentTag(mCurrentTag);
        }
    }

    private void renderXPost(int position, ReaderXPostViewHolder holder) {
        final ReaderPost post = getItem(position);

        if (post.hasPostAvatar()) {
            holder.imgAvatar.setImageUrl(
                    post.getPostAvatarForDisplay(mAvatarSzSmall), WPNetworkImageView.ImageType.AVATAR);
        } else {
            holder.imgAvatar.showDefaultGravatarImage();
        }

        if (post.hasBlogUrl()) {
            holder.imgBlavatar.setImageUrl(
                    post.getPostBlavatarForDisplay(mAvatarSzMedium), WPNetworkImageView.ImageType.BLAVATAR);
        } else {
            holder.imgBlavatar.showDefaultBlavatarImage();
        }

        holder.txtTitle.setText(ReaderXPostUtils.getXPostTitle(post));
        holder.txtSubtitle.setText(ReaderXPostUtils.getXPostSubtitleHtml(post));

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPostSelectedListener != null) {
                    mPostSelectedListener.onPostSelected(post);
                }
            }
        });

        checkLoadMore(position);
    }

    private void renderPost(int position, ReaderPostViewHolder holder) {
        final ReaderPost post = getItem(position);
        ReaderTypes.ReaderPostListType postListType = getPostListType();

        holder.txtTitle.setText(post.getTitle());

        // dateline includes author name if different than blog name
        String dateLine;
        if (post.hasAuthorName() && !post.getAuthorName().equalsIgnoreCase(post.getBlogName())) {
            dateLine = post.getAuthorName() + " \u2022 " + DateTimeUtils.javaDateToTimeSpan(post.getDatePublished());
        } else {
            dateLine = DateTimeUtils.javaDateToTimeSpan(post.getDatePublished());
        }

        // when post header is visible (which it shouldn't be for blog preview), the dateline
        // appears within it - otherwise a separate dateline appears below the title
        if (!isBlogPreview()) {
            holder.txtDate.setText(dateLine);
            // show blog preview when post header is tapped
            holder.layoutPostHeader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReaderActivityLauncher.showReaderBlogPreview(view.getContext(), post);
                }
            });
        } else {
            holder.txtDateBelowTitle.setText(dateLine);
        }

        if (post.hasBlogUrl()) {
            String imageUrl = GravatarUtils.blavatarFromUrl(post.getUrl(), mAvatarSzMedium);
            holder.imgAvatar.setImageUrl(imageUrl, WPNetworkImageView.ImageType.BLAVATAR);
        } else {
            holder.imgAvatar.setImageUrl(post.getPostAvatarForDisplay(mAvatarSzMedium), WPNetworkImageView.ImageType.AVATAR);
        }
        if (post.hasBlogName()) {
            holder.txtBlogName.setText(post.getBlogName());
        } else if (post.hasAuthorName()) {
            holder.txtBlogName.setText(post.getAuthorName());
        } else {
            holder.txtBlogName.setText(null);
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
        } else if (post.hasFeaturedVideo() && WPNetworkImageView.canShowVideoThumbnail(post.getFeaturedVideo())) {
            holder.imgFeatured.setVideoUrl(post.postId, post.getFeaturedVideo());
            holder.imgFeatured.setVisibility(View.VISIBLE);
            titleMargin = mMarginLarge;
        } else {
            holder.imgFeatured.setVisibility(View.GONE);
            titleMargin = (holder.layoutPostHeader.getVisibility() == View.VISIBLE ? 0 : mMarginLarge);
        }

        // set the top margin of the title based on whether there's a featured image and post header
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.txtTitle.getLayoutParams();
        params.topMargin = titleMargin;

        // show word count when appropriate, include reading time if at least two minutes
        if (post.wordCount > 0 && !post.isDiscoverPost()) {
            String wordCountStr = String.format(mWordCountFmtStr, post.wordCount);
            int readingTimeInMinutes = post.wordCount / READING_WORDS_PER_MINUTE;
            if (readingTimeInMinutes >= MIN_READING_TIME_MINUTES) {
                wordCountStr += " (~" + String.format(mReadingTimeFmtStr, readingTimeInMinutes) + ")";
            }
            holder.txtWordCount.setText(wordCountStr);
            holder.txtWordCount.setVisibility(View.VISIBLE);
        } else {
            holder.txtWordCount.setVisibility(View.GONE);
        }

        // show the best tag for this post
        final String tagToDisplay = (mCurrentTag != null ? post.getTagForDisplay(mCurrentTag.getTagSlug()) : null);
        if (!TextUtils.isEmpty(tagToDisplay)) {
            holder.txtTag.setText(ReaderUtils.makeHashTag(tagToDisplay));
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

        showLikes(holder, post);
        showComments(holder, post);

        // more menu only shows for followed tags
        if (!mIsLoggedOutReader && postListType == ReaderTypes.ReaderPostListType.TAG_FOLLOWED) {
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

        // attribution section for discover posts
        if (post.isDiscoverPost()) {
            showDiscoverData(holder, post);
        } else {
            holder.layoutDiscover.setVisibility(View.GONE);
        }

        // if this post has attachments or contains a gallery, scan it for images and show a
        // thumbnail strip of them - note that the thumbnail strip will take care of making
        // itself visible
        if (post.hasAttachments() || post.isGallery()) {
            holder.thumbnailStrip.loadThumbnails(post.blogId, post.postId, post.isPrivate);
        } else {
            holder.thumbnailStrip.setVisibility(View.GONE);
        }

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPostSelectedListener != null) {
                    mPostSelectedListener.onPostSelected(post);
                }
            }
        });

        checkLoadMore(position);
    }

    /*
     * if we're nearing the end of the posts, fire request to load more
     */
    private void checkLoadMore(int position) {
        if (mCanRequestMorePosts
                && mDataRequestedListener != null
                && (position >= getItemCount() - 1)) {
            mDataRequestedListener.onRequestData();
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

        setHasStableIds(true);
    }

    private boolean hasCustomFirstItem() {
        return isBlogPreview() || isTagPreview();
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

    public void clear() {
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
        if (position == 0 && hasCustomFirstItem()) {
            return null;
        }
        if (position == mGapMarkerPosition) {
            return null;
        }

        int arrayPos = hasCustomFirstItem() ? position - 1 : position;

        if (mGapMarkerPosition > -1 && position > mGapMarkerPosition) {
            arrayPos--;
        }

        return mPosts.get(arrayPos);
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

    private void showLikes(final ReaderPostViewHolder holder, final ReaderPost post) {
        boolean canShowLikes;
        if (post.isDiscoverPost()) {
            canShowLikes = false;
        } else if (mIsLoggedOutReader) {
            canShowLikes = post.numLikes > 0;
        } else {
            canShowLikes = post.canLikePost();
        }

        if (canShowLikes) {
            holder.likeCount.setCount(post.numLikes);
            holder.likeCount.setSelected(post.isLikedByCurrentUser);
            holder.likeCount.setVisibility(View.VISIBLE);
            // can't like when logged out
            if (!mIsLoggedOutReader) {
                holder.likeCount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleLike(v.getContext(), holder, post);
                    }
                });
            }
        } else {
            holder.likeCount.setVisibility(View.GONE);
            holder.likeCount.setOnClickListener(null);
        }
    }

    private void showComments(final ReaderPostViewHolder holder, final ReaderPost post) {
        boolean canShowComments;
        if (post.isDiscoverPost()) {
            canShowComments = false;
        } else if (mIsLoggedOutReader) {
            canShowComments = post.numReplies > 0;
        } else {
            canShowComments = post.isWP() && !post.isJetpack && (post.isCommentsOpen || post.numReplies > 0);
        }

        if (canShowComments) {
            holder.commentCount.setCount(post.numReplies);
            holder.commentCount.setVisibility(View.VISIBLE);
            holder.commentCount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.showReaderComments(v.getContext(), post.blogId, post.postId);
                }
            });
        } else {
            holder.commentCount.setVisibility(View.GONE);
            holder.commentCount.setOnClickListener(null);
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
            AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.READER_ARTICLE_LIKED, mCurrentBlogId != 0 ? mCurrentBlogId : null);
            // Consider a like to be enough to push a page view - solves a long-standing question
            // from folks who ask 'why do I have more likes than page views?'.
            ReaderPostActions.bumpPageViewForPost(post);
        } else {
            AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.READER_ARTICLE_LIKED, mCurrentBlogId != 0 ? mCurrentBlogId : null);
        }

        // update post in array and on screen
        int position = mPosts.indexOfPost(post);
        ReaderPost updatedPost = ReaderPostTable.getPost(post.blogId, post.postId, true);
        if (updatedPost != null && position > -1) {
            mPosts.set(position, updatedPost);
            showLikes(holder, updatedPost);
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

    public void removeGapMarker() {
        if (mGapMarkerPosition == -1) return;

        int position = mGapMarkerPosition;
        mGapMarkerPosition = -1;
        if (position < getItemCount()) {
            notifyItemRemoved(position);
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
                case SEARCH_RESULTS:
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

            // determine whether a gap marker exists - only applies to tagged posts
            mGapMarkerPosition = getGapMarkerPosition();

            return true;
        }

        private int getGapMarkerPosition() {
            if (!getPostListType().isTagType()) {
                return -1;
            }

            ReaderBlogIdPostId gapMarkerIds = ReaderPostTable.getGapMarkerIdsForTag(mCurrentTag);
            if (gapMarkerIds == null) {
                return -1;
            }

            // find the position of the gap marker post
            int gapPosition = allPosts.indexOfIds(gapMarkerIds);
            if (gapPosition > -1) {
                // increment it because we want the gap marker to appear *below* this post
                gapPosition++;
                // increment it again if there's a custom first item
                if (hasCustomFirstItem()) {
                    gapPosition++;
                }
                // remove the gap marker if it's on the last post (edge case but
                // it can happen following a purge)
                if (gapPosition >= allPosts.size() - 1) {
                    gapPosition = -1;
                    AppLog.w(AppLog.T.READER, "gap marker at/after last post, removed");
                    ReaderPostTable.removeGapMarkerForTag(mCurrentTag);
                } else {
                    AppLog.d(AppLog.T.READER, "gap marker at position " + gapPosition);
                }
            }
            return gapPosition;
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

