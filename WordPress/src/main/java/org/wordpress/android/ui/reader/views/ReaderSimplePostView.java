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
import org.wordpress.android.ui.reader.models.ReaderSimplePost;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * single simple post view
 */
public class ReaderSimplePostView extends LinearLayout {

    public interface OnSimplePostClickListener {
        void onSimplePostClick(View v, long siteId, long postId);
    }

    private ReaderSimplePost mSimplePost;

    public ReaderSimplePostView(Context context) {
        super(context);
        initView(context);
    }

    public ReaderSimplePostView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderSimplePostView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReaderSimplePostView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.reader_simple_post_view, this);
    }

    public void showPost(ReaderSimplePost simplePost,
                         ViewGroup parent,
                         boolean isGlobal,
                         final OnSimplePostClickListener listener) {
        mSimplePost = simplePost;
        int avatarSize = DisplayUtils.dpToPx(getContext(), getResources().getDimensionPixelSize(R.dimen.avatar_sz_extra_small));

        LayoutInflater inflater = LayoutInflater.from(getContext());

        View postView = inflater.inflate(R.layout.reader_simple_post_view, parent, false);
        TextView txtTitle = (TextView) postView.findViewById(R.id.text_simple_post_title);
        TextView txtExcerpt = (TextView) postView.findViewById(R.id.text_simple_post_excerpt);
        View siteHeader = postView.findViewById(R.id.layout_simple_post_site_header);

        txtTitle.setText(mSimplePost.getTitle());

        if (mSimplePost.hasExcerpt()) {
            txtExcerpt.setText(mSimplePost.getExcerpt());
            txtExcerpt.setVisibility(View.VISIBLE);
        } else {
            txtExcerpt.setVisibility(View.GONE);
        }

        // site header only appears for global related posts
        if (isGlobal) {
            WPNetworkImageView imgAvatar = (WPNetworkImageView) siteHeader.findViewById(R.id.image_avatar);
            TextView txtSiteName = (TextView) siteHeader.findViewById(R.id.text_site_name);
            TextView txtAuthorName = (TextView) siteHeader.findViewById(R.id.text_author_name);
            txtSiteName.setText(mSimplePost.getSiteName());
            txtAuthorName.setText(mSimplePost.getAuthorName());
            if (mSimplePost.hasAuthorAvatarUrl()) {
                imgAvatar.setVisibility(View.VISIBLE);
                String avatarUrl = GravatarUtils.fixGravatarUrl(mSimplePost.getAuthorAvatarUrl(), avatarSize);
                imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
            } else {
                imgAvatar.setVisibility(View.GONE);
            }

            ReaderFollowButton btnFollow = (ReaderFollowButton) siteHeader.findViewById(R.id.simple_post_follow_button);
            btnFollow.setIsFollowed(mSimplePost.isFollowing());
            btnFollow.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowStatus((ReaderFollowButton) v);
                }
            });

            siteHeader.setVisibility(View.VISIBLE);
        } else {
            siteHeader.setVisibility(View.GONE);
        }

        showFeaturedImage(postView);

        postView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onSimplePostClick(view,
                            mSimplePost.getSiteId(),
                            mSimplePost.getPostId());
                }
            }
        });

        parent.addView(postView);
    }

    private void toggleFollowStatus(final ReaderFollowButton btnFollow) {
        if (!NetworkUtils.checkConnection(getContext())) return;

        final boolean isAskingToFollow = !mSimplePost.isFollowing();

        ReaderActions.ActionListener listener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (getContext() == null) return;

                btnFollow.setEnabled(true);
                if (succeeded) {
                    mSimplePost.setIsFollowing(isAskingToFollow);
                } else {
                    int errResId = isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog;
                    ToastUtils.showToast(getContext(), errResId);
                    btnFollow.setIsFollowed(!isAskingToFollow);
                }
            }
        };

        // disable follow button until call completes
        btnFollow.setEnabled(false);

        ReaderBlogActions.followBlogById(mSimplePost.getSiteId(), isAskingToFollow, listener);
        btnFollow.setIsFollowedAnimated(isAskingToFollow);
    }

    private void showFeaturedImage(final View postView) {
        final WPNetworkImageView imgFeatured = (WPNetworkImageView) postView.findViewById(R.id.image_featured);

        // post must have an excerpt in order to show featured image (not enough space otherwise)
        if (!mSimplePost.hasFeaturedImageUrl() || !mSimplePost.hasExcerpt()) {
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
                int featuredImageWidth = DisplayUtils.dpToPx(
                        getContext(),
                        getContext().getResources().getDimensionPixelSize(R.dimen.reader_simple_post_image_width));
                int cropWidth = featuredImageWidth;
                int cropHeight = postView.getHeight();
                String photonUrl = PhotonUtils.getPhotonImageUrl(
                        mSimplePost.getFeaturedImageUrl(), cropWidth, cropHeight);
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
}
