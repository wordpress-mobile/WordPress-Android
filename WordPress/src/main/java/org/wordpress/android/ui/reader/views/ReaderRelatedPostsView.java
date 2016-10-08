package org.wordpress.android.ui.reader.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
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
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions.RelatedPostsType;
import org.wordpress.android.ui.reader.models.ReaderRelatedPost;
import org.wordpress.android.ui.reader.models.ReaderRelatedPostList;
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
        mFeaturedImageWidth = DisplayUtils.dpToPx(getContext(), getResources().getDimensionPixelSize(R.dimen.reader_related_post_image_width));
    }

    public void setOnRelatedPostClickListener(OnRelatedPostClickListener listener) {
        mClickListener = listener;
    }

    public void showRelatedPosts(ReaderRelatedPostList relatedPosts,
                                 RelatedPostsType relatedPostsType,
                                 String siteName) {

        if (relatedPosts.size() == 0) {
            return;
        }

        ViewGroup container = (ViewGroup) findViewById(R.id.container_related_posts);
        container.removeAllViews();

        int avatarSize = DisplayUtils.dpToPx(getContext(), getResources().getDimensionPixelSize(R.dimen.avatar_sz_extra_small));
        boolean isGlobal = relatedPostsType == ReaderPostActions.RelatedPostsType.GLOBAL;

        // add a separate view for each related post
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int index = 0; index < relatedPosts.size(); index++) {
            final ReaderRelatedPost relatedPost = relatedPosts.get(index);

            View postView = inflater.inflate(R.layout.reader_related_post, container, false);
            TextView txtTitle = (TextView) postView.findViewById(R.id.text_related_post_title);
            TextView txtExcerpt = (TextView) postView.findViewById(R.id.text_related_post_excerpt);
            WPNetworkImageView imgFeatured = (WPNetworkImageView) postView.findViewById(R.id.image_featured);
            View siteHeader = postView.findViewById(R.id.layout_related_post_site_header);

            txtTitle.setText(relatedPost.getTitle());

            if (relatedPost.hasExcerpt()) {
                txtExcerpt.setText(relatedPost.getExcerpt());
                txtExcerpt.setVisibility(View.VISIBLE);
            } else {
                txtExcerpt.setVisibility(View.GONE);
            }

            // site header only appears for global posts
            if (isGlobal) {
                WPNetworkImageView imgAvatar = (WPNetworkImageView) siteHeader.findViewById(R.id.image_avatar);
                TextView txtSiteName = (TextView) siteHeader.findViewById(R.id.text_site_name);
                TextView txtAuthorName = (TextView) siteHeader.findViewById(R.id.text_author_name);
                txtSiteName.setText(relatedPost.getSiteName());
                txtAuthorName.setText(relatedPost.getAuthorName());
                if (relatedPost.hasAuthorAvatarUrl()) {
                    String avatarUrl = GravatarUtils.fixGravatarUrl(relatedPost.getAuthorAvatarUrl(), avatarSize);
                    imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
                } else {
                    imgAvatar.showDefaultGravatarImage();
                }
                siteHeader.setVisibility(View.VISIBLE);
            } else {
                siteHeader.setVisibility(View.GONE);
            }

            final ReaderFollowButton btnFollow = (ReaderFollowButton) postView.findViewById(R.id.related_post_follow_button);
            btnFollow.setIsFollowed(relatedPost.isFollowing());
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowStatus(btnFollow, relatedPost);
                }
            });

            showFeaturedImage(relatedPost, postView);

            postView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mClickListener != null) {
                        mClickListener.onRelatedPostClick(view, relatedPost.getSiteId(), relatedPost.getPostId());
                    }
                }
            });

            container.addView(postView);
        }

        // make sure the label for these related posts is showing
        TextView label = (TextView) findViewById(R.id.text_related_posts_label);
        if (isGlobal) {
            label.setText(getContext().getString(R.string.reader_label_global_related_posts));
        } else {
            label.setText(String.format(getContext().getString(R.string.reader_label_local_related_posts), siteName));
        }
    }

    private void toggleFollowStatus(final ReaderFollowButton btnFollow, ReaderRelatedPost relatedPost) {
        if (!NetworkUtils.checkConnection(getContext())) return;

        final boolean isAskingToFollow = !relatedPost.isFollowing();

        ReaderActions.ActionListener listener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (getContext() == null) return;

                btnFollow.setEnabled(true);
                if (!succeeded) {
                    int errResId = isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog;
                    ToastUtils.showToast(getContext(), errResId);
                    btnFollow.setIsFollowed(!isAskingToFollow);
                }
            }
        };

        // disable follow button until API call returns
        btnFollow.setEnabled(false);
        boolean result = ReaderBlogActions.followBlogById(relatedPost.getSiteId(), isAskingToFollow, listener);
        if (result) {
            btnFollow.setIsFollowedAnimated(isAskingToFollow);
        }
    }

    private void showFeaturedImage(final ReaderRelatedPost relatedPost, final View postView) {
        final WPNetworkImageView imgFeatured = (WPNetworkImageView) postView.findViewById(R.id.image_featured);

        // post must have an excerpt in order to show featured image
        if (!relatedPost.hasFeaturedImageUrl() || !relatedPost.hasExcerpt()) {
            imgFeatured.setVisibility(View.GONE);
            return;
        }

        // featured image has height set to MATCH_PARENT so wait for parent's layout to complete
        // before loading image so we can set the image height correctly
        postView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                postView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                String photonUrl = PhotonUtils.getPhotonImageUrl(relatedPost.getFeaturedImageUrl(), mFeaturedImageWidth, postView.getHeight());
                imgFeatured.setImageUrl(photonUrl, WPNetworkImageView.ImageType.PHOTO);
            }
        });
        imgFeatured.setVisibility(View.VISIBLE);
    }

}
