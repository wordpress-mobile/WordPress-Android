package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderThumbnailTable;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.ReaderCardType;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostDiscoverData;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.news.NewsItem;
import org.wordpress.android.ui.news.NewsViewHolder;
import org.wordpress.android.ui.news.NewsViewHolder.NewsCardListener;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderAnim;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.ReaderInterfaces;
import org.wordpress.android.ui.reader.ReaderInterfaces.OnFollowListener;
import org.wordpress.android.ui.reader.ReaderInterfaces.ReblogActionListener;
import org.wordpress.android.ui.reader.ReaderTypes;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils;
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils.VideoThumbnailUrlListener;
import org.wordpress.android.ui.reader.utils.ReaderXPostUtils;
import org.wordpress.android.ui.reader.views.ReaderFollowButton;
import org.wordpress.android.ui.reader.views.ReaderGapMarkerView;
import org.wordpress.android.ui.reader.views.ReaderIconCountView;
import org.wordpress.android.ui.reader.views.ReaderSiteHeaderView;
import org.wordpress.android.ui.reader.views.ReaderTagHeaderView;
import org.wordpress.android.ui.reader.views.ReaderThumbnailStrip;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ColorUtils;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ViewUtilsKt;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.HashSet;

import javax.inject.Inject;

public class ReaderPostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final ImageManager mImageManager;
    private NewsCardListener mNewsCardListener;
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
    private NewsItem mNewsItem;

    private ReaderInterfaces.OnFollowListener mFollowListener;
    private ReaderInterfaces.OnPostSelectedListener mPostSelectedListener;
    private ReaderInterfaces.OnPostPopupListener mOnPostPopupListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;
    private ReaderInterfaces.OnPostBookmarkedListener mOnPostBookmarkedListener;
    private ReaderActions.DataRequestedListener mDataRequestedListener;
    private ReaderSiteHeaderView.OnBlogInfoLoadedListener mBlogInfoLoadedListener;
    private ReblogActionListener mReblogActionListener;

    // the large "tbl_posts.text" column is unused here, so skip it when querying
    private static final boolean EXCLUDE_TEXT_COLUMN = true;
    private static final int MAX_ROWS = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY;

    private static final int VIEW_TYPE_POST = 0;
    private static final int VIEW_TYPE_XPOST = 1;
    private static final int VIEW_TYPE_SITE_HEADER = 2;
    private static final int VIEW_TYPE_TAG_HEADER = 3;
    private static final int VIEW_TYPE_GAP_MARKER = 4;
    private static final int VIEW_TYPE_REMOVED_POST = 5;
    private static final int VIEW_TYPE_NEWS_CARD = 6;

    private static final long ITEM_ID_HEADER = -1L;
    private static final long ITEM_ID_GAP_MARKER = -2L;
    private static final long ITEM_ID_NEWS_CARD = -3L;

    private static final int NEWS_CARD_POSITION = 0;

    private boolean mIsMainReader = false;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    /*
     * cross-post
     */
    private class ReaderXPostViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mImgAvatar;
        private final ImageView mImgBlavatar;
        private final TextView mTxtTitle;
        private final TextView mTxtSubtitle;

        ReaderXPostViewHolder(View itemView) {
            super(itemView);
            View postContainer = itemView.findViewById(R.id.post_container);
            mImgAvatar = itemView.findViewById(R.id.image_avatar);
            mImgBlavatar = itemView.findViewById(R.id.image_blavatar);
            mTxtTitle = itemView.findViewById(R.id.text_title);
            mTxtSubtitle = itemView.findViewById(R.id.text_subtitle);

            postContainer.setOnClickListener(new View.OnClickListener() {
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

    private class ReaderRemovedPostViewHolder extends RecyclerView.ViewHolder {
        final View mPostContainer;

        private final ViewGroup mRemovedPostContainer;
        private final TextView mTxtRemovedPostTitle;
        private final TextView mUndoRemoveAction;

        ReaderRemovedPostViewHolder(View itemView) {
            super(itemView);
            mPostContainer = itemView.findViewById(R.id.post_container);
            mTxtRemovedPostTitle = itemView.findViewById(R.id.removed_post_title);
            mRemovedPostContainer = itemView.findViewById(R.id.removed_item_container);
            mUndoRemoveAction = itemView.findViewById(R.id.undo_remove);
        }
    }

    /*
     * full post
     */
    private class ReaderPostViewHolder extends RecyclerView.ViewHolder {
        final View mPostContainer;

        private final TextView mTxtTitle;
        private final TextView mTxtText;
        private final TextView mTxtAuthorAndBlogName;
        private final TextView mTxtDateline;

        private final ReaderIconCountView mReblog;
        private final ReaderIconCountView mCommentCount;
        private final ReaderIconCountView mLikeCount;
        private final ImageView mBtnBookmark;

        private final ImageView mImgMore;
        private final ImageView mImgVideoOverlay;
        private final View mVisit;

        private final ImageView mImgFeatured;
        private final ImageView mImgAvatarOrBlavatar;

        private final ReaderFollowButton mFollowButton;

        private final Group mFramePhoto;
        private final TextView mTxtPhotoTitle;

        private final Group mLayoutDiscover;
        private final ImageView mImgDiscoverAvatar;
        private final TextView mTxtDiscover;

        private final ReaderThumbnailStrip mThumbnailStrip;


        ReaderPostViewHolder(View itemView) {
            super(itemView);

            mPostContainer = itemView.findViewById(R.id.post_container);

            mTxtTitle = itemView.findViewById(R.id.text_title);
            mTxtText = itemView.findViewById(R.id.text_excerpt);
            mTxtAuthorAndBlogName = itemView.findViewById(R.id.text_author_and_blog_name);
            mTxtDateline = itemView.findViewById(R.id.text_dateline);

            mReblog = itemView.findViewById(R.id.reblog);
            mCommentCount = itemView.findViewById(R.id.count_comments);
            mLikeCount = itemView.findViewById(R.id.count_likes);
            mBtnBookmark = itemView.findViewById(R.id.bookmark);

            mFramePhoto = itemView.findViewById(R.id.frame_photo);
            mTxtPhotoTitle = itemView.findViewById(R.id.text_photo_title);
            mImgFeatured = itemView.findViewById(R.id.image_featured);
            mImgVideoOverlay = itemView.findViewById(R.id.image_video_overlay);

            mImgAvatarOrBlavatar = itemView.findViewById(R.id.image_avatar_or_blavatar);
            mImgMore = itemView.findViewById(R.id.image_more);
            mVisit = itemView.findViewById(R.id.visit);

            mLayoutDiscover = itemView.findViewById(R.id.layout_discover);
            mImgDiscoverAvatar = itemView.findViewById(R.id.image_discover_avatar);
            mTxtDiscover = itemView.findViewById(R.id.text_discover);

            mThumbnailStrip = itemView.findViewById(R.id.thumbnail_strip);

            View postHeaderView = itemView.findViewById(R.id.layout_post_header);
            mFollowButton = itemView.findViewById(R.id.follow_button);

            ViewUtilsKt.expandTouchTargetArea(mLayoutDiscover, R.dimen.reader_discover_layout_extra_padding, true);
            ViewUtilsKt.expandTouchTargetArea(mVisit, R.dimen.reader_visit_layout_extra_padding, false);
            ViewUtilsKt.expandTouchTargetArea(mImgMore, R.dimen.reader_more_image_extra_padding, false);

            // show post in internal browser when "visit" is clicked
            View.OnClickListener visitListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    ReaderPost post = getItem(position);
                    if (post != null) {
                        AnalyticsTracker.track(Stat.READER_ARTICLE_VISITED);
                        ReaderActivityLauncher.openPost(view.getContext(), post);
                    }
                }
            };
            mVisit.setOnClickListener(visitListener);

            // show author/blog link as disabled if we're previewing a blog, otherwise show
            // blog preview when the post header is clicked
            if (getPostListType() == ReaderTypes.ReaderPostListType.BLOG_PREVIEW) {
                int color = ContextExtensionsKt.getColorFromAttribute(itemView.getContext(), R.attr.colorOnSurface);
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
        int headerPosition = hasNewsCard() ? 1 : 0;
        if (position == NEWS_CARD_POSITION && hasNewsCard()) {
            return VIEW_TYPE_NEWS_CARD;
        } else if (position == headerPosition && hasSiteHeader()) {
            // first item is a ReaderSiteHeaderView
            return VIEW_TYPE_SITE_HEADER;
        } else if (position == headerPosition && hasTagHeader()) {
            // first item is a ReaderTagHeaderView
            return VIEW_TYPE_TAG_HEADER;
        } else if (position == mGapMarkerPosition) {
            return VIEW_TYPE_GAP_MARKER;
        } else {
            ReaderPost post = getItem(position);
            if (post != null && post.isXpost()) {
                return VIEW_TYPE_XPOST;
            } else if (post != null && isBookmarksList() && !post.isBookmarked) {
                return VIEW_TYPE_REMOVED_POST;
            } else {
                return VIEW_TYPE_POST;
            }
        }
    }

    @Override
    public @NonNull RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View postView;
        switch (viewType) {
            case VIEW_TYPE_NEWS_CARD:
                return new NewsViewHolder(parent, mNewsCardListener);
            case VIEW_TYPE_SITE_HEADER:
                ReaderSiteHeaderView readerSiteHeaderView = new ReaderSiteHeaderView(context);
                readerSiteHeaderView.setOnFollowListener(mFollowListener);
                return new SiteHeaderViewHolder(readerSiteHeaderView);

            case VIEW_TYPE_TAG_HEADER:
                return new TagHeaderViewHolder(new ReaderTagHeaderView(context));

            case VIEW_TYPE_GAP_MARKER:
                return new GapMarkerViewHolder(new ReaderGapMarkerView(context));

            case VIEW_TYPE_XPOST:
                postView = LayoutInflater.from(context).inflate(R.layout.reader_cardview_xpost, parent, false);
                return new ReaderXPostViewHolder(postView);
            case VIEW_TYPE_REMOVED_POST:
                postView = LayoutInflater.from(context).inflate(R.layout.reader_cardview_removed_post, parent, false);
                return new ReaderRemovedPostViewHolder(postView);
            default:
                postView = LayoutInflater.from(context).inflate(R.layout.reader_cardview_post, parent, false);
                return new ReaderPostViewHolder(postView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ReaderPostViewHolder) {
            renderPost(position, (ReaderPostViewHolder) holder);
        } else if (holder instanceof ReaderXPostViewHolder) {
            renderXPost(position, (ReaderXPostViewHolder) holder);
        } else if (holder instanceof ReaderRemovedPostViewHolder) {
            renderRemovedPost(position, (ReaderRemovedPostViewHolder) holder);
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
        } else if (holder instanceof NewsViewHolder) {
            ((NewsViewHolder) holder).bind(mNewsItem);
        }
    }

    private void renderXPost(int position, ReaderXPostViewHolder holder) {
        final ReaderPost post = getItem(position);
        if (post == null) {
            return;
        }

        mImageManager
                .loadIntoCircle(holder.mImgAvatar, ImageType.AVATAR,
                        GravatarUtils.fixGravatarUrl(post.getPostAvatar(), mAvatarSzSmall));

        mImageManager.load(holder.mImgBlavatar, ImageType.BLAVATAR,
                GravatarUtils.fixGravatarUrl(post.getBlogImageUrl(), mAvatarSzSmall));

        holder.mTxtTitle.setText(ReaderXPostUtils.getXPostTitle(post));
        holder.mTxtSubtitle.setText(ReaderXPostUtils.getXPostSubtitleHtml(post));

        checkLoadMore(position);
    }

    private void renderRemovedPost(final int position, final ReaderRemovedPostViewHolder holder) {
        final ReaderPost post = getItem(position);
        final Context context = holder.mRemovedPostContainer.getContext();
        holder.mTxtRemovedPostTitle.setText(createTextForRemovedPostContainer(post, context));
        Drawable drawable =
                ColorUtils.INSTANCE.applyTintToDrawable(context, R.drawable.ic_undo_white_24dp,
                        ContextExtensionsKt.getColorResIdFromAttribute(context, R.attr.colorPrimary));
        holder.mUndoRemoveAction.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        holder.mPostContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoPostUnbookmarked(post, position);
            }
        });
    }

    private void undoPostUnbookmarked(final ReaderPost post, final int position) {
        if (!post.isBookmarked) {
            toggleBookmark(post.blogId, post.postId);
            notifyItemChanged(position);
        }
    }

    private void renderPost(final int position, final ReaderPostViewHolder holder) {
        final ReaderPost post = getItem(position);
        ReaderPostListType postListType = getPostListType();
        if (post == null) {
            return;
        }

        holder.mTxtDateline.setText(DateTimeUtils.javaDateToTimeSpan(post.getDisplayDate(), WordPress.getContext()));

        if (post.hasBlogImageUrl()) {
            String imageUrl = GravatarUtils.fixGravatarUrl(post.getBlogImageUrl(), mAvatarSzMedium);
            mImageManager.loadIntoCircle(holder.mImgAvatarOrBlavatar, ImageType.BLAVATAR, imageUrl);
            holder.mImgAvatarOrBlavatar.setVisibility(View.VISIBLE);
        } else if (post.hasPostAvatar()) {
            String imageUrl = GravatarUtils.fixGravatarUrl(post.getPostAvatar(), mAvatarSzMedium);
            mImageManager.loadIntoCircle(holder.mImgAvatarOrBlavatar,
                    ImageType.AVATAR, imageUrl);
            holder.mImgAvatarOrBlavatar.setBackgroundColor(0);
            holder.mImgAvatarOrBlavatar.setVisibility(View.VISIBLE);
        } else {
            mImageManager.cancelRequestAndClearImageView(holder.mImgAvatarOrBlavatar);
            holder.mImgAvatarOrBlavatar.setVisibility(View.GONE);
        }

        /*if (post.hasBlogName() && post.hasAuthorName() && !post.getBlogName().equals(post.getAuthorName())) {
            holder.mTxtAuthorAndBlogName.setText(holder.mTxtAuthorAndBlogName.getResources()
                                                                             .getString(R.string.author_name_blog_name,
                                                                                     post.getAuthorName(),
                                                                                     post.getBlogName()));
        } else */ if (post.hasBlogName()) {
            holder.mTxtAuthorAndBlogName.setText(post.getBlogName());
         /*} else if (post.hasAuthorName()) {
            holder.mTxtAuthorAndBlogName.setText(post.getAuthorName());*/
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
            mImageManager.load(holder.mImgFeatured, ImageType.PHOTO,
                    post.getFeaturedImageForDisplay(mPhotonWidth, mPhotonHeight), ScaleType.CENTER_CROP);
            holder.mThumbnailStrip.setVisibility(View.GONE);
        } else {
            mImageManager.cancelRequestAndClearImageView(holder.mImgFeatured);
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
                ReaderVideoUtils.retrieveVideoThumbnailUrl(post.getFeaturedVideo(), new VideoThumbnailUrlListener() {
                    @Override public void showThumbnail(String thumbnailUrl) {
                        mImageManager.load(holder.mImgFeatured, ImageType.PHOTO, thumbnailUrl, ScaleType.CENTER_CROP);
                    }

                    @Override public void showPlaceholder() {
                        mImageManager.load(holder.mImgFeatured, ImageType.VIDEO);
                    }

                    @Override public void cacheThumbnailUrl(String thumbnailUrl) {
                        ReaderThumbnailTable.addThumbnail(post.postId, post.getFeaturedVideo(), thumbnailUrl);
                    }
                });
                holder.mFramePhoto.setVisibility(View.VISIBLE);
                holder.mThumbnailStrip.setVisibility(View.GONE);
                titleMargin = mMarginLarge;
            } else if (post.hasFeaturedImage()) {
                mImageManager.load(holder.mImgFeatured, ImageType.PHOTO,
                        post.getFeaturedImageForDisplay(mPhotonWidth, mPhotonHeight), ScaleType.CENTER_CROP);
                holder.mFramePhoto.setVisibility(View.VISIBLE);
                holder.mThumbnailStrip.setVisibility(View.GONE);
                titleMargin = mMarginLarge;
            } else {
                holder.mFramePhoto.setVisibility(View.GONE);
                holder.mThumbnailStrip.setVisibility(View.GONE);
                titleMargin = 0;
            }

            // set the top margin of the title based on whether there's a featured image
            LayoutParams params = (LayoutParams) holder.mTxtTitle.getLayoutParams();
            params.topMargin = titleMargin;
        }

        // show the video overlay (play icon) when there's a featured video
        holder.mImgVideoOverlay.setVisibility(post.getCardType() == ReaderCardType.VIDEO ? View.VISIBLE : View.GONE);

        showLikes(holder, post);
        showComments(holder, post);
        showReblogButton(holder, post);
        initBookmarkButton(position, holder, post);

        // more menu only shows for followed tags
        if (!mIsLoggedOutReader && postListType == ReaderPostListType.TAG_FOLLOWED) {
            holder.mImgMore.setVisibility(View.VISIBLE);
            holder.mImgMore.setOnClickListener(new OnClickListener() {
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
            holder.mFollowButton.setOnClickListener(new OnClickListener() {
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

        holder.mPostContainer.setOnClickListener(new OnClickListener() {
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
                mImageManager.loadIntoCircle(postHolder.mImgDiscoverAvatar, ImageType.AVATAR,
                        GravatarUtils.fixGravatarUrl(discoverData.getAvatarUrl(), mAvatarSzSmall));
                // tapping an editor pick opens the source post, which is handled by the existing
                // post selection handler
                for (int id : postHolder.mLayoutDiscover.getReferencedIds()) {
                    postHolder.itemView.findViewById(id).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mPostSelectedListener != null) {
                                mPostSelectedListener.onPostSelected(post);
                            }
                        }
                    });
                }
                break;

            case SITE_PICK:
                // BLAVATAR
                mImageManager.load(postHolder.mImgDiscoverAvatar, ImageType.BLAVATAR,
                        GravatarUtils.fixGravatarUrl(discoverData.getAvatarUrl(), mAvatarSzSmall));
                // site picks show "Visit [BlogName]" link - tapping opens the blog preview if
                // we have the blogId, if not show blog in internal webView
                for (int id : postHolder.mLayoutDiscover.getReferencedIds()) {
                    postHolder.itemView.findViewById(id).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (discoverData.getBlogId() != 0) {
                                ReaderActivityLauncher.showReaderBlogPreview(v.getContext(), discoverData.getBlogId());
                            } else if (discoverData.hasBlogUrl()) {
                                ReaderActivityLauncher.openUrl(v.getContext(), discoverData.getBlogUrl());
                            }
                        }
                    });
                }
                break;

            case OTHER:
            default:
                mImageManager.cancelRequestAndClearImageView(postHolder.mImgDiscoverAvatar);
                // something else, so hide discover section
                postHolder.mLayoutDiscover.setVisibility(View.GONE);
                break;
        }
    }

    // ********************************************************************************************

    public ReaderPostAdapter(
            Context context,
            ReaderPostListType postListType,
            ImageManager imageManager,
            boolean isMainReader
    ) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);
        this.mImageManager = imageManager;
        mPostListType = postListType;
        mAvatarSzMedium = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
        mAvatarSzSmall = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
        mMarginLarge = context.getResources().getDimensionPixelSize(R.dimen.margin_large);
        mIsLoggedOutReader = !mAccountStore.hasAccessToken();
        mIsMainReader = isMainReader;

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int cardMargin = context.getResources().getDimensionPixelSize(R.dimen.reader_card_margin);
        mPhotonWidth = displayWidth - (cardMargin * 2);
        mPhotonHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height_cardview);

        setHasStableIds(true);
    }

    private boolean hasHeader() {
        return hasSiteHeader() || hasTagHeader();
    }

    private boolean hasSiteHeader() {
        return !mIsMainReader && (isDiscover() || getPostListType() == ReaderTypes.ReaderPostListType.BLOG_PREVIEW);
    }

    private boolean hasTagHeader() {
        return !mIsMainReader && (mCurrentTag != null && mCurrentTag.isTagTopic() && !isEmpty());
    }

    private boolean isDiscover() {
        return mCurrentTag != null && mCurrentTag.isDiscover();
    }

    public void setOnFollowListener(OnFollowListener listener) {
        mFollowListener = listener;
    }

    public void setReblogActionListener(ReblogActionListener reblogActionListener) {
        mReblogActionListener = reblogActionListener;
    }

    public void setOnPostSelectedListener(ReaderInterfaces.OnPostSelectedListener listener) {
        mPostSelectedListener = listener;
    }

    public void setOnDataLoadedListener(ReaderInterfaces.DataLoadedListener listener) {
        mDataLoadedListener = listener;
    }

    public void setOnPostBookmarkedListener(ReaderInterfaces.OnPostBookmarkedListener listener) {
        mOnPostBookmarkedListener = listener;
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

    public void setOnNewsCardListener(NewsCardListener newsCardListener) {
        this.mNewsCardListener = newsCardListener;
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
        mGapMarkerPosition = -1;
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
        if (position == NEWS_CARD_POSITION && hasNewsCard()) {
            return null;
        }
        if (position == getHeaderPosition() && hasHeader()) {
            return null;
        }
        if (position == mGapMarkerPosition) {
            return null;
        }

        int arrayPos = position - getItemPositionOffset();

        if (mGapMarkerPosition > -1 && position > mGapMarkerPosition) {
            arrayPos--;
        }

        if (mPosts.size() <= arrayPos) {
            AppLog.d(T.READER, "Trying to read an element out of bounds of the posts list");
            return null;
        }

        return mPosts.get(arrayPos);
    }

    private int getItemPositionOffset() {
        int newsCardOffset = hasNewsCard() ? 1 : 0;
        int headersOffset = hasHeader() ? 1 : 0;
        return newsCardOffset + headersOffset;
    }

    private int getHeaderPosition() {
        int headerPosition = hasNewsCard() ? 1 : 0;
        return hasHeader() ? headerPosition : -1;
    }

    @Override
    public int getItemCount() {
        int size = mPosts.size();
        if (mGapMarkerPosition != -1) {
            size++;
        }
        if (hasHeader()) {
            size++;
        }
        if (hasNewsCard()) {
            size++;
        }
        return size;
    }

    public boolean isEmpty() {
        return (mPosts == null || mPosts.size() == 0);
    }

    private boolean isBookmarksList() {
        return (getPostListType() == ReaderPostListType.TAG_FOLLOWED
                && (mCurrentTag != null && mCurrentTag.isBookmarked()));
    }

    @Override
    public long getItemId(int position) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_TAG_HEADER:
            case VIEW_TYPE_SITE_HEADER:
                return ITEM_ID_HEADER;
            case VIEW_TYPE_GAP_MARKER:
                return ITEM_ID_GAP_MARKER;
            case VIEW_TYPE_NEWS_CARD:
                return ITEM_ID_NEWS_CARD;
            default:
                ReaderPost post = getItem(position);
                return post != null ? post.getStableId() : 0;
        }
    }

    private void showLikes(final ReaderPostViewHolder holder, final ReaderPost post) {
        boolean canShowLikes;
        if (post.isDiscoverPost() || isBookmarksList()) {
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
            holder.mLikeCount.setContentDescription(ReaderUtils.getLongLikeLabelText(holder.mPostContainer.getContext(),
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

    private void initBookmarkButton(final int position, final ReaderPostViewHolder holder, final ReaderPost post) {
        updateBookmarkView(holder, post);
        holder.mBtnBookmark.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                toggleBookmark(post.blogId, post.postId);
                if (isBookmarksList()) {
                    // automatically starts the expand/collapse animations
                    notifyItemChanged(position);
                } else {
                    // notifyItemChanged highlights the item for a bit, we need to manually updateTheView to prevent it
                    updateBookmarkView(holder, getItem(position));
                }
            }
        });
    }

    private void updateBookmarkView(final ReaderPostViewHolder holder, final ReaderPost post) {
        final ImageView bookmarkButton = holder.mBtnBookmark;
        Context context = holder.mBtnBookmark.getContext();

        boolean canBookmarkPost = !post.isDiscoverPost();
        if (canBookmarkPost) {
            bookmarkButton.setVisibility(View.VISIBLE);
        } else {
            bookmarkButton.setVisibility(View.GONE);
        }

        if (post.isBookmarked) {
            bookmarkButton.setSelected(true);
            bookmarkButton.setContentDescription(context.getString(R.string.reader_remove_bookmark));
        } else {
            bookmarkButton.setSelected(false);
            bookmarkButton.setContentDescription(context.getString(R.string.reader_add_bookmark));
        }
    }

    /**
     * Creates 'Removed [post title]' text, with the '[post title]' in bold.
     */
    @NonNull
    private SpannableStringBuilder createTextForRemovedPostContainer(ReaderPost post, Context context) {
        String removedString = context.getString(R.string.removed);
        String removedPostTitle = removedString + " " + post.getTitle();
        SpannableStringBuilder str = new SpannableStringBuilder(removedPostTitle);
        str.setSpan(new StyleSpan(Typeface.BOLD), removedString.length(), removedPostTitle.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return str;
    }

    private void showComments(final ReaderPostViewHolder holder, final ReaderPost post) {
        boolean canShowComments;
        if (post.isDiscoverPost() || isBookmarksList()) {
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

    /**
     * Sets reblog button visibility and action
     *
     * @param holder the view holder
     * @param post   the current reader post
     */
    private void showReblogButton(final ReaderPostViewHolder holder, final ReaderPost post) {
        boolean canBeReblogged = !mIsLoggedOutReader && !post.isPrivate;
        if (canBeReblogged) {
            holder.mReblog.setCount(0);
            holder.mReblog.setVisibility(View.VISIBLE);
            holder.mReblog.setOnClickListener(v -> mReblogActionListener.reblog(post));
        } else {
            holder.mReblog.setVisibility(View.GONE);
            holder.mReblog.setOnClickListener(null);
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
     * triggered when user taps the bookmark post button
     */
    private void toggleBookmark(final long blogId, final long postId) {
        ReaderPost post = ReaderPostTable.getBlogPost(blogId, postId, false);

        AnalyticsTracker.Stat eventToTrack;
        if (post.isBookmarked) {
            eventToTrack = isBookmarksList() ? AnalyticsTracker.Stat.READER_POST_UNSAVED_FROM_SAVED_POST_LIST
                    : AnalyticsTracker.Stat.READER_POST_UNSAVED_FROM_OTHER_POST_LIST;
            ReaderPostActions.removeFromBookmarked(post);
        } else {
            eventToTrack = isBookmarksList() ? AnalyticsTracker.Stat.READER_POST_SAVED_FROM_SAVED_POST_LIST
                    : AnalyticsTracker.Stat.READER_POST_SAVED_FROM_OTHER_POST_LIST;
            ReaderPostActions.addToBookmarked(post);
        }

        AnalyticsTracker.track(eventToTrack);

        // update post in array and on screen
        post = ReaderPostTable.getBlogPost(blogId, postId, true);
        int position = mPosts.indexOfPost(post);
        if (post != null && position > -1) {
            mPosts.set(position, post);

            if (mOnPostBookmarkedListener != null) {
                mOnPostBookmarkedListener
                        .onBookmarkedStateChanged(post.isBookmarked, blogId, postId, !isBookmarksList());
            }
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

        if (mFollowListener != null) {
            if (isAskingToFollow) {
                mFollowListener.onFollowTapped(followButton, post.getBlogName(), post.blogId);
            } else {
                mFollowListener.onFollowingTapped();
            }
        }

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

    public void updateNewsCardItem(NewsItem newsItem) {
        NewsItem prevState = mNewsItem;
        mNewsItem = newsItem;
        if (prevState == null && newsItem != null) {
            notifyItemInserted(NEWS_CARD_POSITION);
        } else if (prevState != null) {
            if (newsItem == null) {
                notifyItemRemoved(NEWS_CARD_POSITION);
            } else {
                notifyItemChanged(NEWS_CARD_POSITION);
            }
        }
    }

    private boolean hasNewsCard() {
        // We don't want to display the card when we are displaying just a loading screen. However, on Discover a header
        // is shown, even when we are loading data, so the card should be displayed. [moreover displaying the card only
        // after we fetch the data results in weird animation after configuration change, since it plays insertion
        // animation for all the data (including the card) except of the header which hasn't changed].
        return mNewsItem != null && (!isEmpty() || isDiscover());
    }

    /*
     * AsyncTask to load posts in the current tag
     */
    private boolean mIsTaskRunning = false;

    private class LoadPostsTask extends AsyncTask<Void, Void, Boolean> {
        private ReaderPostList mAllPosts;

        private boolean mCanRequestMorePostsTemp;
        private int mGapMarkerPositionTemp;

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

            if (mPosts.isSameListWithBookmark(mAllPosts)) {
                return false;
            }

            // if we're not already displaying the max # posts, enable requesting more when
            // the user scrolls to the end of the list
            mCanRequestMorePostsTemp = (numExisting < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY);

            // determine whether a gap marker exists - only applies to tagged posts
            mGapMarkerPositionTemp = getGapMarkerPosition();

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

            int gapMarkerPostPosition = mAllPosts.indexOfIds(gapMarkerIds);
            int gapMarkerPosition = -1;
            if (gapMarkerPostPosition > -1) {
                // remove the gap marker if it's on the last post (edge case but
                // it can happen following a purge)
                if (gapMarkerPostPosition == mAllPosts.size() - 1) {
                    AppLog.w(AppLog.T.READER, "gap marker at/after last post, removed");
                    ReaderPostTable.removeGapMarkerForTag(mCurrentTag);
                } else {
                    // we want the gap marker to appear *below* this post
                    gapMarkerPosition = gapMarkerPostPosition + 1;
                    // increment it if there are custom items at the top of the list (header or newsCard)
                    gapMarkerPosition += getItemPositionOffset();
                    AppLog.d(AppLog.T.READER, "gap marker at position " + gapMarkerPostPosition);
                }
            }
            return gapMarkerPosition;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                ReaderPostAdapter.this.mGapMarkerPosition = mGapMarkerPositionTemp;
                ReaderPostAdapter.this.mCanRequestMorePosts = mCanRequestMorePostsTemp;
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
