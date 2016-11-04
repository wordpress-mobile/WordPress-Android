package org.wordpress.android.ui.reader.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.models.ReaderRelatedPost;
import org.wordpress.android.ui.reader.models.ReaderRelatedPostList;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * used by the detail view to display related posts, which can be either local (related posts
 * from the same site as the source post) or global (related posts from across wp.com)
 */
public class ReaderRelatedPostsView extends LinearLayout {

    public interface OnRelatedPostClickListener {
        void onRelatedPostClick(View v, long siteId, long postId);
    }

    private OnRelatedPostClickListener mClickListener;
    private int mFeaturedImageWidth;
    private final ReaderRelatedPostList mRelatedPostList = new ReaderRelatedPostList();

    public ReaderRelatedPostsView(Context context) {
        super(context);
        initView(context);
    }

    public ReaderRelatedPostsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderRelatedPostsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReaderRelatedPostsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.reader_related_posts_view, this);
        mFeaturedImageWidth = DisplayUtils.dpToPx(context, context.getResources().getDimensionPixelSize(R.dimen.reader_related_post_image_width));
    }

    public void setOnRelatedPostClickListener(OnRelatedPostClickListener listener) {
        mClickListener = listener;
    }

    public void showRelatedPosts(@NonNull ReaderRelatedPostList posts, String siteName, boolean isGlobal) {
        mRelatedPostList.clear();
        mRelatedPostList.addAll(posts);

        ViewGroup container = (ViewGroup) findViewById(R.id.container_related_posts);
        container.removeAllViews();

        // nothing more to do if passed list is empty
        if (mRelatedPostList.size() == 0) return;

        int avatarSize = DisplayUtils.dpToPx(getContext(), getResources().getDimensionPixelSize(R.dimen.avatar_sz_extra_small));

        // add a separate view for each related post
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int index = 0; index < mRelatedPostList.size(); index++) {
            final int position = index;
            ReaderRelatedPost relatedPost = mRelatedPostList.get(position);

            View postView = inflater.inflate(R.layout.reader_related_post, container, false);
            TextView txtTitle = (TextView) postView.findViewById(R.id.text_related_post_title);
            TextView txtExcerpt = (TextView) postView.findViewById(R.id.text_related_post_excerpt);
            View siteHeader = postView.findViewById(R.id.layout_related_post_site_header);

            txtTitle.setText(relatedPost.getTitle());

            if (relatedPost.hasExcerpt()) {
                txtExcerpt.setText(relatedPost.getExcerpt());
                txtExcerpt.setVisibility(View.VISIBLE);
            } else {
                txtExcerpt.setVisibility(View.GONE);
            }

            // site header only appears for global related posts
            if (isGlobal) {
                WPNetworkImageView imgAvatar = (WPNetworkImageView) siteHeader.findViewById(R.id.image_avatar);
                TextView txtSiteName = (TextView) siteHeader.findViewById(R.id.text_site_name);
                TextView txtAuthorName = (TextView) siteHeader.findViewById(R.id.text_author_name);
                txtSiteName.setText(relatedPost.getSiteName());
                txtAuthorName.setText(relatedPost.getAuthorName());
                if (relatedPost.hasAuthorAvatarUrl()) {
                    imgAvatar.setVisibility(View.VISIBLE);
                    String avatarUrl = GravatarUtils.fixGravatarUrl(relatedPost.getAuthorAvatarUrl(), avatarSize);
                    imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
                } else {
                    imgAvatar.setVisibility(View.GONE);
                }

                final ReaderFollowButton btnFollow = (ReaderFollowButton) siteHeader.findViewById(R.id.related_post_follow_button);
                btnFollow.setIsFollowed(relatedPost.isFollowing());
                btnFollow.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleFollowStatus(btnFollow, position);
                    }
                });

                siteHeader.setVisibility(View.VISIBLE);
            } else {
                siteHeader.setVisibility(View.GONE);
            }

            showFeaturedImage(postView, relatedPost);

            postView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mClickListener != null) {
                        mClickListener.onRelatedPostClick(view,
                                mRelatedPostList.get(position).getSiteId(),
                                mRelatedPostList.get(position).getPostId());
                    }
                }
            });

            container.addView(postView);
        }

        // make sure the label for these related posts has the correct caption
        TextView label = (TextView) findViewById(R.id.text_related_posts_label);
        if (isGlobal) {
            label.setText(getContext().getString(R.string.reader_label_global_related_posts));
        } else {
            label.setText(String.format(getContext().getString(R.string.reader_label_local_related_posts), siteName));
        }
    }

    /**
     * user tapped follow button on a global related post
     * @param btnFollow Follow button for the site to follow
     * @param position index of the related post in mRelatedPosts
     */
    private void toggleFollowStatus(final ReaderFollowButton btnFollow, final int position) {
        if (!NetworkUtils.checkConnection(getContext())) return;

        final boolean isAskingToFollow = !mRelatedPostList.get(position).isFollowing();

        ReaderActions.ActionListener listener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (getContext() == null) return;

                btnFollow.setEnabled(true);
                if (succeeded) {
                    mRelatedPostList.get(position).setIsFollowing(isAskingToFollow);
                } else {
                    int errResId = isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog;
                    ToastUtils.showToast(getContext(), errResId);
                    btnFollow.setIsFollowed(!isAskingToFollow);
                }
            }
        };

        // disable follow button until call completes
        btnFollow.setEnabled(false);

        ReaderBlogActions.followBlogById(mRelatedPostList.get(position).getSiteId(), isAskingToFollow, listener);
        btnFollow.setIsFollowedAnimated(isAskingToFollow);
    }

    /**
     * shows the featured image for the passed related post, if available
     * @param postView parent view which contains the featured image and other related post views
     * @param relatedPost related post to operate on
     */
    private void showFeaturedImage(final View postView, final ReaderRelatedPost relatedPost) {
        final WPNetworkImageView imgFeatured = (WPNetworkImageView) postView.findViewById(R.id.image_featured);

        // post must have an excerpt in order to show featured image (not enough space otherwise)
        if (!relatedPost.hasFeaturedImageUrl() || !relatedPost.hasExcerpt()) {
            imgFeatured.setVisibility(View.GONE);
            return;
        }

        // featured image has height set to MATCH_PARENT so wait for parent view's layout to complete
        // before loading image so we can set the image height correctly, then tell the imageView
        // to crop the downloaded image to fit the exact width/height of the view
        postView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                postView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int cropWidth = mFeaturedImageWidth;
                int cropHeight = postView.getHeight();
                String photonUrl = PhotonUtils.getPhotonImageUrl(
                        relatedPost.getFeaturedImageUrl(), cropWidth, cropHeight);
                imgFeatured.setImageUrl(
                        photonUrl,
                        WPNetworkImageView.ImageType.PHOTO,
                        null,
                        cropWidth,
                        cropHeight);
            }
        });

        imgFeatured.setVisibility(View.VISIBLE);
    }

    /*
     * called by reader detail when this related posts view is scrolled into view, tracks
     * railcar events for each related post
     */
    public void trackRailcarRender() {
        for (ReaderRelatedPost post: mRelatedPostList) {
            AnalyticsUtils.trackRailcarRender(post.getRailcarJson());
        }
    }

}
