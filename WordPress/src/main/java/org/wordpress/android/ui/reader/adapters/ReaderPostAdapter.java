package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.ReaderCardType;
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
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.utils.ReaderXPostUtils;
import org.wordpress.android.ui.reader.views.ReaderFollowButton;
import org.wordpress.android.ui.reader.views.ReaderGapMarkerView;
import org.wordpress.android.ui.reader.views.ReaderIconCountView;
import org.wordpress.android.ui.reader.views.ReaderSiteHeaderView;
import org.wordpress.android.ui.reader.views.ReaderTagHeaderView;
import org.wordpress.android.ui.reader.views.ReaderThumbnailStrip;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.HashSet;

import javax.inject.Inject;

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

    private boolean mCanRequestMorePosts;
    private final boolean mIsLoggedOutReader;

    private final ReaderTypes.ReaderPostListType mPostListType;
    private final ReaderPostList mPosts = new ReaderPostList();
    private final HashSet<String> mRenderedIds = new HashSet<>();

    private ReaderInterfaces.OnPostSelectedListener mPostSelectedListener;
    private ReaderInterfaces.OnPostPopupListener mOnPostPopupListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;
    private ReaderActions.DataRequestedListener mDataRequestedListener;
    private ReaderSiteHeaderView.OnBlogInfoLoadedListener mBlogInfoLoadedListener;

    // the large "tbl_posts.text" column is unused here, so skip it when querying
    private static final boolean EXCLUDE_TEXT_COLUMN = true;
    private static final int MAX_ROWS = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY;

    private static final int VIEW_TYPE_POST = 0;
    private static final int VIEW_TYPE_XPOST = 1;
    private static final int VIEW_TYPE_SITE_HEADER = 2;
    private static final int VIEW_TYPE_TAG_HEADER = 3;
    private static final int VIEW_TYPE_GAP_MARKER = 4;

    private static final long ITEM_ID_HEADER = -1L;
    private static final long ITEM_ID_GAP_MARKER = -2L;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    /*
     * cross-post
     */
    private class ReaderXPostViewHolder extends RecyclerView.ViewHolder {
        private final CardView mCardView;
        private final WPNetworkImageView mImgAvatar;
        private final WPNetworkImageView mImgBlavatar;
        private final TextView mTxtTitle;
        private final TextView mTxtSubtitle;

        ReaderXPostViewHolder(View itemView) {
            super(itemView);
            mCardView = (CardView) itemView.findViewById(R.id.card_view);
            mImgAvatar = (WPNetworkImageView) itemView.findViewById(R.id.image_avatar);
            mImgBlavatar = (WPNetworkImageView) itemView.findViewById(R.id.image_blavatar);
            mTxtTitle = (TextView) itemView.findViewById(R.id.text_title);
            mTxtSubtitle = (TextView) itemView.findViewById(R.id.text_subtitle);

            mCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    ReaderPost post = getItem(position);
                    if (mPostSelectedListener != null && post != null) {
                        mPostSelectedListener.onPostSelected(post);
                    }
                }
            });
        }
    }

    /*
     * full post
     */
    private class ReaderPostViewHolder extends RecyclerView.ViewHolder {
        final CardView mCardView;

        private final TextView mTxtTitle;
        private final TextView mTxtText;
        private final TextView mTxtAuthorAndBlogName;
        private final TextView mTxtDateline;

        private final ReaderIconCountView mCommentCount;
        private final ReaderIconCountView mLikeCount;

        private final ImageView mImgMore;
        private final ImageView mImgVideoOverlay;
        private final LinearLayout mVisit;

        private final WPNetworkImageView mImgFeatured;
        private final WPNetworkImageView mImgAvatarOrBlavatar;

        private final ReaderFollowButton mFollowButton;

        private final ViewGroup mFramePhoto;
        private final TextView mTxtPhotoTitle;

        private final ViewGroup mLayoutDiscover;
        private final WPNetworkImageView mImgDiscoverAvatar;
        private final TextView mTxtDiscover;

        private final ReaderThumbnailStrip mThumbnailStrip;

        ReaderPostViewHolder(View itemView) {
            super(itemView);

            mCardView = (CardView) itemView.findViewById(R.id.card_view);

            mTxtTitle = (TextView) itemView.findViewById(R.id.text_title);
            mTxtText = (TextView) itemView.findViewById(R.id.text_excerpt);
            mTxtAuthorAndBlogName = (TextView) itemView.findViewById(R.id.text_author_and_blog_name);
            mTxtDateline = (TextView) itemView.findViewById(R.id.text_dateline);

            mCommentCount = (ReaderIconCountView) itemView.findViewById(R.id.count_comments);
            mLikeCount = (ReaderIconCountView) itemView.findViewById(R.id.count_likes);

            mFramePhoto = (ViewGroup) itemView.findViewById(R.id.frame_photo);
            mTxtPhotoTitle = (TextView) mFramePhoto.findViewById(R.id.text_photo_title);
            mImgFeatured = (WPNetworkImageView) mFramePhoto.findViewById(R.id.image_featured);
            mImgVideoOverlay = (ImageView) mFramePhoto.findViewById(R.id.image_video_overlay);

            mImgAvatarOrBlavatar = (WPNetworkImageView) itemView.findViewById(R.id.image_avatar_or_blavatar);
            mImgMore = (ImageView) itemView.findViewById(R.id.image_more);
            mVisit = (LinearLayout) itemView.findViewById(R.id.visit);

            mLayoutDiscover = (ViewGroup) itemView.findViewById(R.id.layout_discover);
            mImgDiscoverAvatar = (WPNetworkImageView) mLayoutDiscover.findViewById(R.id.image_discover_avatar);
            mTxtDiscover = (TextView) mLayoutDiscover.findViewById(R.id.text_discover);

            mThumbnailStrip = (ReaderThumbnailStrip) itemView.findViewById(R.id.thumbnail_strip);

            ViewGroup postHeaderView = (ViewGroup) itemView.findViewById(R.id.layout_post_header);
            mFollowButton = (ReaderFollowButton) postHeaderView.findViewById(R.id.follow_button);

            // show post in internal browser when "visit" is clicked
            View.OnClickListener visitListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    ReaderPost post = getItem(position);
                    if (post != null) {
                        AnalyticsTracker.track(Stat.READER_ARTICLE_VIEWED);
                        ReaderActivityLauncher.openUrl(view.getContext(), post.getUrl());
                    }
                }
            };
            mVisit.setOnClickListener(visitListener);

            // show author/blog link as disabled if we're previewing a blog, otherwise show
            // blog preview when the post header is clicked
            if (getPostListType() == ReaderTypes.ReaderPostListType.BLOG_PREVIEW) {
                int color = itemView.getContext().getResources().getColor(R.color.grey_dark);
                mTxtAuthorAndBlogName.setTextColor(color);
                // remove the ripple background
                postHeaderView.setBackground(null);
            } else {
                postHeaderView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position = getAdapterPosition();
                        ReaderPost post = getItem(position);
                        if (post != null) {
                            ReaderActivityLauncher.showReaderBlogPreview(view.getContext(), post);
                        }
                    }
                });
            }

            // play the featured video when the overlay image is tapped - note that the overlay
            // image only appears when there's a featured video
            mImgVideoOverlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    ReaderPost post = getItem(position);
                    if (post != null && post.hasFeaturedVideo()) {
                        ReaderActivityLauncher.showReaderVideoViewer(view.getContext(), post.getFeaturedVideo());
                    }
                }
            });

            ReaderUtils.setBackgroundToRoundRipple(mImgMore);
        }
    }

    private class SiteHeaderViewHolder extends RecyclerView.ViewHolder {
        private final ReaderSiteHeaderView mSiteHeaderView;

        SiteHeaderViewHolder(View itemView) {
            super(itemView);
            mSiteHeaderView = (ReaderSiteHeaderView) itemView;
        }
    }

    private class TagHeaderViewHolder extends RecyclerView.ViewHolder {
        private final ReaderTagHeaderView mTagHeaderView;

        TagHeaderViewHolder(View itemView) {
            super(itemView);
            mTagHeaderView = (ReaderTagHeaderView) itemView;
        }
    }

    private class GapMarkerViewHolder extends RecyclerView.ViewHolder {
        private final ReaderGapMarkerView mGapMarkerView;

        GapMarkerViewHolder(View itemView) {
            super(itemView);
            mGapMarkerView = (ReaderGapMarkerView) itemView;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && hasSiteHeader()) {
            // first item is a ReaderSiteHeaderView
            return VIEW_TYPE_SITE_HEADER;
        } else if (position == 0 && hasTagHeader()) {
            // first item is a ReaderTagHeaderView
            return VIEW_TYPE_TAG_HEADER;
        } else if (position == mGapMarkerPosition) {
            return VIEW_TYPE_GAP_MARKER;
        } else {
            ReaderPost post = getItem(position);
            if (post != null && post.isXpost()) {
                return VIEW_TYPE_XPOST;
            } else {
                return VIEW_TYPE_POST;
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        switch (viewType) {
            case VIEW_TYPE_SITE_HEADER:
                return new SiteHeaderViewHolder(new ReaderSiteHeaderView(context));

            case VIEW_TYPE_TAG_HEADER:
                return new TagHeaderViewHolder(new ReaderTagHeaderView(context));

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
        } else if (holder instanceof SiteHeaderViewHolder) {
            SiteHeaderViewHolder siteHolder = (SiteHeaderViewHolder) holder;
            siteHolder.mSiteHeaderView.setOnBlogInfoLoadedListener(mBlogInfoLoadedListener);
            if (isDiscover()) {
                siteHolder.mSiteHeaderView.loadBlogInfo(ReaderConstants.DISCOVER_SITE_ID, 0);
            } else {
                siteHolder.mSiteHeaderView.loadBlogInfo(mCurrentBlogId, mCurrentFeedId);
            }
        } else if (holder instanceof TagHeaderViewHolder) {
            TagHeaderViewHolder tagHolder = (TagHeaderViewHolder) holder;
            tagHolder.mTagHeaderView.setCurrentTag(mCurrentTag);
        } else if (holder instanceof GapMarkerViewHolder) {
            GapMarkerViewHolder gapHolder = (GapMarkerViewHolder) holder;
            gapHolder.mGapMarkerView.setCurrentTag(mCurrentTag);
        }
    }

    private void renderXPost(int position, ReaderXPostViewHolder holder) {
        final ReaderPost post = getItem(position);
        if (post == null) {
            return;
        }

        if (post.hasPostAvatar()) {
            holder.mImgAvatar.setImageUrl(
                    GravatarUtils.fixGravatarUrl(post.getPostAvatar(), mAvatarSzSmall),
                    WPNetworkImageView.ImageType.AVATAR);
        } else {
            holder.mImgAvatar.showDefaultGravatarImageAndNullifyUrl();
        }

        if (post.hasBlogImageUrl()) {
            holder.mImgBlavatar.setImageUrl(
                    GravatarUtils.fixGravatarUrl(post.getBlogImageUrl(), mAvatarSzSmall),
                    WPNetworkImageView.ImageType.BLAVATAR);
        } else {
            holder.mImgBlavatar.showDefaultBlavatarImageAndNullifyUrl();
        }

        holder.mTxtTitle.setText(ReaderXPostUtils.getXPostTitle(post));
        holder.mTxtSubtitle.setText(ReaderXPostUtils.getXPostSubtitleHtml(post));

        checkLoadMore(position);
    }

    private void renderPost(int position, ReaderPostViewHolder holder) {
        final ReaderPost post = getItem(position);
        ReaderTypes.ReaderPostListType postListType = getPostListType();
        if (post == null) {
            return;
        }

        holder.mTxtDateline.setText(DateTimeUtils.javaDateToTimeSpan(post.getDisplayDate(), WordPress.getContext()));

        // show avatar if it exists, otherwise show blavatar
        if (post.hasPostAvatar()) {
            String imageUrl = GravatarUtils.fixGravatarUrl(post.getPostAvatar(), mAvatarSzMedium);
            holder.mImgAvatarOrBlavatar.setImageUrl(imageUrl, WPNetworkImageView.ImageType.AVATAR);
            holder.mImgAvatarOrBlavatar.setVisibility(View.VISIBLE);
        } else if (post.hasBlogImageUrl()) {
            String imageUrl = GravatarUtils.fixGravatarUrl(post.getBlogImageUrl(), mAvatarSzMedium);
            holder.mImgAvatarOrBlavatar.setImageUrl(imageUrl, WPNetworkImageView.ImageType.BLAVATAR);
            holder.mImgAvatarOrBlavatar.setVisibility(View.VISIBLE);
        } else {
            holder.mImgAvatarOrBlavatar.setVisibility(View.GONE);
        }

        // show author and blog name if both are available, otherwise show whichever is available
        if (post.hasBlogName() && post.hasAuthorName() && !post.getBlogName().equals(post.getAuthorName())) {
            holder.mTxtAuthorAndBlogName.setText(holder.mTxtAuthorAndBlogName.getResources()
                                                                             .getString(R.string.author_name_blog_name,
                                                                                     post.getAuthorName(),
                                                                                     post.getBlogName()));
        } else if (post.hasBlogName()) {
            holder.mTxtAuthorAndBlogName.setText(post.getBlogName());
        } else if (post.hasAuthorName()) {
            holder.mTxtAuthorAndBlogName.setText(post.getAuthorName());
        } else {
            holder.mTxtAuthorAndBlogName.setText(null);
        }

        if (post.getCardType() == ReaderCardType.PHOTO) {
            // posts with a suitable featured image that have very little text get the "photo
            // card" treatment - show the title overlaid on the featured image without any text
            holder.mTxtText.setVisibility(View.GONE);
            holder.mTxtTitle.setVisibility(View.GONE);
            holder.mFramePhoto.setVisibility(View.VISIBLE);
            holder.mTxtPhotoTitle.setVisibility(View.VISIBLE);
            holder.mTxtPhotoTitle.setText(post.getTitle());
            holder.mImgFeatured.setImageUrl(
                    post.getFeaturedImageForDisplay(mPhotonWidth, mPhotonHeight),
                    WPNetworkImageView.ImageType.PHOTO);
            holder.mThumbnailStrip.setVisibility(View.GONE);
        } else {
            holder.mTxtTitle.setVisibility(View.VISIBLE);
            holder.mTxtTitle.setText(post.getTitle());
            holder.mTxtPhotoTitle.setVisibility(View.GONE);

            if (post.hasExcerpt()) {
                holder.mTxtText.setVisibility(View.VISIBLE);
                holder.mTxtText.setText(post.getExcerpt());
            } else {
                holder.mTxtText.setVisibility(View.GONE);
            }

            final int titleMargin;
            if (post.getCardType() == ReaderCardType.GALLERY) {
                // if this post is a gallery, scan it for images and show a thumbnail strip of
                // them - note that the thumbnail strip will take care of making itself visible
                holder.mThumbnailStrip.loadThumbnails(post.blogId, post.postId, post.isPrivate);
                holder.mFramePhoto.setVisibility(View.GONE);
                titleMargin = mMarginLarge;
            } else if (post.getCardType() == ReaderCardType.VIDEO) {
                holder.mImgFeatured.setVideoUrl(post.postId, post.getFeaturedVideo());
                holder.mFramePhoto.setVisibility(View.VISIBLE);
                holder.mThumbnailStrip.setVisibility(View.GONE);
                titleMargin = mMarginLarge;
            } else if (post.hasFeaturedImage()) {
                holder.mImgFeatured.setImageUrl(
                        post.getFeaturedImageForDisplay(mPhotonWidth, mPhotonHeight),
                        WPNetworkImageView.ImageType.PHOTO);
                holder.mFramePhoto.setVisibility(View.VISIBLE);
                holder.mThumbnailStrip.setVisibility(View.GONE);
                titleMargin = mMarginLarge;
            } else {
                holder.mFramePhoto.setVisibility(View.GONE);
                holder.mThumbnailStrip.setVisibility(View.GONE);
                titleMargin = 0;
            }

            // set the top margin of the title based on whether there's a featured image
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.mTxtTitle.getLayoutParams();
            params.topMargin = titleMargin;
        }

        // show the video overlay (play icon) when there's a featured video
        holder.mImgVideoOverlay.setVisibility(post.getCardType() == ReaderCardType.VIDEO ? View.VISIBLE : View.GONE);

        showLikes(holder, post);
        showComments(holder, post);

        // more menu only shows for followed tags
        if (!mIsLoggedOutReader && postListType == ReaderTypes.ReaderPostListType.TAG_FOLLOWED) {
            holder.mImgMore.setVisibility(View.VISIBLE);
            holder.mImgMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnPostPopupListener != null) {
                        mOnPostPopupListener.onShowPostPopup(view, post);
                    }
                }
            });
        } else {
            holder.mImgMore.setVisibility(View.GONE);
            holder.mImgMore.setOnClickListener(null);
        }

        if (shouldShowFollowButton()) {
            holder.mFollowButton.setIsFollowed(post.isFollowedByCurrentUser);
            holder.mFollowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleFollow(view.getContext(), view, post);
                }
            });
            holder.mFollowButton.setVisibility(View.VISIBLE);
        } else {
            holder.mFollowButton.setVisibility(View.GONE);
        }

        // attribution section for discover posts
        if (post.isDiscoverPost()) {
            showDiscoverData(holder, post);
        } else {
            holder.mLayoutDiscover.setVisibility(View.GONE);
        }

        holder.mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPostSelectedListener != null) {
                    mPostSelectedListener.onPostSelected(post);
                }
            }
        });

        checkLoadMore(position);

        // if we haven't already rendered this post and it has a "railcar" attached to it, add it
        // to the rendered list and record the TrainTracks render event
        if (post.hasRailcar() && !mRenderedIds.contains(post.getPseudoId())) {
            mRenderedIds.add(post.getPseudoId());
            AnalyticsUtils.trackRailcarRender(post.getRailcarJson());
        }
    }

    /*
     * follow button only shows for tags and "Posts I Like" - it doesn't show for Followed Sites,
     * Discover, lists, etc.
     */
    private boolean shouldShowFollowButton() {
        return mCurrentTag != null
               && (mCurrentTag.isTagTopic() || mCurrentTag.isPostsILike())
               && !mIsLoggedOutReader;
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
            postHolder.mLayoutDiscover.setVisibility(View.GONE);
            return;
        }

        postHolder.mLayoutDiscover.setVisibility(View.VISIBLE);
        postHolder.mTxtDiscover.setText(discoverData.getAttributionHtml());

        switch (discoverData.getDiscoverType()) {
            case EDITOR_PICK:
                if (discoverData.hasAvatarUrl()) {
                    postHolder.mImgDiscoverAvatar
                            .setImageUrl(GravatarUtils.fixGravatarUrl(discoverData.getAvatarUrl(), mAvatarSzSmall),
                                         WPNetworkImageView.ImageType.AVATAR);
                } else {
                    postHolder.mImgDiscoverAvatar.showDefaultGravatarImageAndNullifyUrl();
                }
                // tapping an editor pick opens the source post, which is handled by the existing
                // post selection handler
                postHolder.mLayoutDiscover.setOnClickListener(new View.OnClickListener() {
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
                    postHolder.mImgDiscoverAvatar.setImageUrl(
                            GravatarUtils.fixGravatarUrl(discoverData.getAvatarUrl(), mAvatarSzSmall),
                            WPNetworkImageView.ImageType.BLAVATAR);
                } else {
                    postHolder.mImgDiscoverAvatar.showDefaultBlavatarImageAndNullifyUrl();
                }
                // site picks show "Visit [BlogName]" link - tapping opens the blog preview if
                // we have the blogId, if not show blog in internal webView
                postHolder.mLayoutDiscover.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (discoverData.getBlogId() != 0) {
                            ReaderActivityLauncher.showReaderBlogPreview(v.getContext(), discoverData.getBlogId());
                        } else if (discoverData.hasBlogUrl()) {
                            ReaderActivityLauncher.openUrl(v.getContext(), discoverData.getBlogUrl());
                        }
                    }
                });
                break;

            default:
                // something else, so hide discover section
                postHolder.mLayoutDiscover.setVisibility(View.GONE);
                break;
        }
    }

    // ********************************************************************************************

    public ReaderPostAdapter(Context context, ReaderTypes.ReaderPostListType postListType) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);

        mPostListType = postListType;
        mAvatarSzMedium = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
        mAvatarSzSmall = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
        mMarginLarge = context.getResources().getDimensionPixelSize(R.dimen.margin_large);
        mIsLoggedOutReader = !mAccountStore.hasAccessToken();

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int cardMargin = context.getResources().getDimensionPixelSize(R.dimen.reader_card_margin);
        mPhotonWidth = displayWidth - (cardMargin * 2);
        mPhotonHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height_cardview);

        setHasStableIds(true);
    }

    private boolean hasCustomFirstItem() {
        return hasSiteHeader() || hasTagHeader();
    }

    private boolean hasSiteHeader() {
        return isDiscover() || getPostListType() == ReaderTypes.ReaderPostListType.BLOG_PREVIEW;
    }

    private boolean hasTagHeader() {
        return mCurrentTag != null && mCurrentTag.isTagTopic() && !isEmpty();
    }

    private boolean isDiscover() {
        return mCurrentTag != null && mCurrentTag.isDiscover();
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

    public void setOnBlogInfoLoadedListener(ReaderSiteHeaderView.OnBlogInfoLoadedListener listener) {
        mBlogInfoLoadedListener = listener;
    }

    private ReaderTypes.ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
    }

    // used when the viewing tagged posts
    public void setCurrentTag(ReaderTag tag) {
        if (!ReaderTag.isSameTag(tag, mCurrentTag)) {
            mCurrentTag = tag;
            mRenderedIds.clear();
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
            mRenderedIds.clear();
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
        switch (getItemViewType(position)) {
            case VIEW_TYPE_TAG_HEADER:
            case VIEW_TYPE_SITE_HEADER:
                return ITEM_ID_HEADER;
            case VIEW_TYPE_GAP_MARKER:
                return ITEM_ID_GAP_MARKER;
            default:
                ReaderPost post = getItem(position);
                return post != null ? post.getStableId() : 0;
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
            holder.mLikeCount.setCount(post.numLikes);
            holder.mLikeCount.setSelected(post.isLikedByCurrentUser);
            holder.mLikeCount.setVisibility(View.VISIBLE);
            holder.mLikeCount.setContentDescription(ReaderUtils.getLongLikeLabelText(holder.mCardView.getContext(),
                                                                                     post.numLikes,
                                                                                     post.isLikedByCurrentUser));
            // can't like when logged out
            if (!mIsLoggedOutReader) {
                holder.mLikeCount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleLike(v.getContext(), holder, post);
                    }
                });
            }
        } else {
            holder.mLikeCount.setVisibility(View.GONE);
            holder.mLikeCount.setOnClickListener(null);
        }
    }

    private void showComments(final ReaderPostViewHolder holder, final ReaderPost post) {
        boolean canShowComments;
        if (post.isDiscoverPost()) {
            canShowComments = false;
        } else if (mIsLoggedOutReader) {
            canShowComments = post.numReplies > 0;
        } else {
            canShowComments = post.isWP() && (post.isCommentsOpen || post.numReplies > 0);
        }

        if (canShowComments) {
            holder.mCommentCount.setCount(post.numReplies);
            holder.mCommentCount.setVisibility(View.VISIBLE);
            holder.mCommentCount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.showReaderComments(v.getContext(), post.blogId, post.postId);
                }
            });
        } else {
            holder.mCommentCount.setVisibility(View.GONE);
            holder.mCommentCount.setOnClickListener(null);
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
        ReaderAnim.animateLikeButton(holder.mLikeCount.getImageView(), isAskingToLike);

        if (!ReaderPostActions.performLikeAction(post, isAskingToLike, mAccountStore.getAccount().getUserId())) {
            ToastUtils.showToast(context, R.string.reader_toast_err_generic);
            return;
        }

        if (isAskingToLike) {
            AnalyticsUtils.trackWithReaderPostDetails(AnalyticsTracker.Stat.READER_ARTICLE_LIKED, post);
            // Consider a like to be enough to push a page view - solves a long-standing question
            // from folks who ask 'why do I have more likes than page views?'.
            ReaderPostActions.bumpPageViewForPost(mSiteStore, post);
        } else {
            AnalyticsUtils.trackWithReaderPostDetails(AnalyticsTracker.Stat.READER_ARTICLE_UNLIKED, post);
        }

        // update post in array and on screen
        int position = mPosts.indexOfPost(post);
        ReaderPost updatedPost = ReaderPostTable.getBlogPost(post.blogId, post.postId, true);
        if (updatedPost != null && position > -1) {
            mPosts.set(position, updatedPost);
            showLikes(holder, updatedPost);
        }
    }

    /*
     * triggered when user taps the follow button on a post
     */
    private void toggleFollow(final Context context, final View followButton, final ReaderPost post) {
        if (post == null || !NetworkUtils.checkConnection(context)) {
            return;
        }

        boolean isCurrentlyFollowed = ReaderPostTable.isPostFollowed(post);
        final boolean isAskingToFollow = !isCurrentlyFollowed;

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                followButton.setEnabled(true);
                if (!succeeded) {
                    int resId = (isAskingToFollow ? R.string.reader_toast_err_follow_blog
                            : R.string.reader_toast_err_unfollow_blog);
                    ToastUtils.showToast(context, resId);
                    setFollowStatusForBlog(post.blogId, !isAskingToFollow);
                }
            }
        };

        if (!ReaderBlogActions.followBlogForPost(post, isAskingToFollow, actionListener)) {
            ToastUtils.showToast(context, R.string.reader_toast_err_generic);
            return;
        }

        followButton.setEnabled(false);
        setFollowStatusForBlog(post.blogId, isAskingToFollow);
    }

    public void setFollowStatusForBlog(long blogId, boolean isFollowing) {
        ReaderPost post;
        for (int i = 0; i < mPosts.size(); i++) {
            post = mPosts.get(i);
            if (post.blogId == blogId && post.isFollowedByCurrentUser != isFollowing) {
                post.isFollowedByCurrentUser = isFollowing;
                mPosts.set(i, post);
                notifyItemChanged(i);
            }
        }
    }

    public void removeGapMarker() {
        if (mGapMarkerPosition == -1) {
            return;
        }

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
        private ReaderPostList mAllPosts;

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
                    mAllPosts = ReaderPostTable.getPostsWithTag(mCurrentTag, MAX_ROWS, EXCLUDE_TEXT_COLUMN);
                    numExisting = ReaderPostTable.getNumPostsWithTag(mCurrentTag);
                    break;
                case BLOG_PREVIEW:
                    if (mCurrentFeedId != 0) {
                        mAllPosts = ReaderPostTable.getPostsInFeed(mCurrentFeedId, MAX_ROWS, EXCLUDE_TEXT_COLUMN);
                        numExisting = ReaderPostTable.getNumPostsInFeed(mCurrentFeedId);
                    } else {
                        mAllPosts = ReaderPostTable.getPostsInBlog(mCurrentBlogId, MAX_ROWS, EXCLUDE_TEXT_COLUMN);
                        numExisting = ReaderPostTable.getNumPostsInBlog(mCurrentBlogId);
                    }
                    break;
                default:
                    return false;
            }

            if (mPosts.isSameList(mAllPosts)) {
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
            int gapPosition = mAllPosts.indexOfIds(gapMarkerIds);
            if (gapPosition > -1) {
                // increment it because we want the gap marker to appear *below* this post
                gapPosition++;
                // increment it again if there's a custom first item
                if (hasCustomFirstItem()) {
                    gapPosition++;
                }
                // remove the gap marker if it's on the last post (edge case but
                // it can happen following a purge)
                if (gapPosition >= mAllPosts.size() - 1) {
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
                mPosts.addAll(mAllPosts);
                notifyDataSetChanged();
            }

            if (mDataLoadedListener != null) {
                mDataLoadedListener.onDataLoaded(isEmpty());
            }

            mIsTaskRunning = false;
        }
    }
}
