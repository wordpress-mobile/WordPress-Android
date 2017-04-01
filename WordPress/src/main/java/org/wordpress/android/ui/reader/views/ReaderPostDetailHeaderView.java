package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * topmost view in post detail - shows blavatar + avatar, author name, blog name, and follow button
 */
public class ReaderPostDetailHeaderView extends LinearLayout {

    private ReaderPost mPost;
    private ReaderFollowButton mFollowButton;
    private boolean mEnableBlogPreview = true;

    public ReaderPostDetailHeaderView(Context context) {
        super(context);
        initView(context);
    }

    public ReaderPostDetailHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderPostDetailHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        View view = inflate(context, R.layout.reader_post_detail_header_view, this);
        mFollowButton = (ReaderFollowButton) view.findViewById(R.id.header_follow_button);
    }

    public void setPost(@NonNull ReaderPost post, boolean signedInWPComEh) {
        mPost = post;

        TextView txtTitle = (TextView) findViewById(R.id.text_header_title);
        TextView txtSubtitle = (TextView) findViewById(R.id.text_header_subtitle);

        boolean blogNameEh = mPost.blogNameEh();
        boolean authorNameEh = mPost.authorNameEh();

        if (blogNameEh && authorNameEh) {
            // don't show author name if it's the same as the blog name
            if (mPost.getAuthorName().equals(mPost.getBlogName())) {
                txtTitle.setText(mPost.getAuthorName());
                txtSubtitle.setVisibility(View.GONE);
            } else {
                txtTitle.setText(mPost.getAuthorName());
                txtSubtitle.setText(mPost.getBlogName());
            }
        } else if (blogNameEh) {
            txtTitle.setText(mPost.getBlogName());
            txtSubtitle.setVisibility(View.GONE);
        } else if (authorNameEh) {
            txtTitle.setText(mPost.getAuthorName());
            txtSubtitle.setVisibility(View.GONE);
        } else {
            txtTitle.setText(R.string.untitled);
            txtSubtitle.setVisibility(View.GONE);
        }

        if (mEnableBlogPreview) {
            txtTitle.setOnClickListener(mClickListener);
            txtSubtitle.setOnClickListener(mClickListener);
        } else {
            int color = getContext().getResources().getColor(R.color.grey_dark);
            txtTitle.setTextColor(color);
            txtSubtitle.setTextColor(color);
        }

        if (signedInWPComEh) {
            mFollowButton.setVisibility(View.VISIBLE);
            mFollowButton.setIsFollowed(mPost.followedByCurrentUserEh);
            mFollowButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowStatus();
                }
            });
        } else {
            mFollowButton.setVisibility(View.GONE);
        }

        showBlavatarAndAvatar(mPost.getBlogImageUrl(), mPost.getPostAvatar());
    }

    private void showBlavatarAndAvatar(String blavatarUrl, String avatarUrl) {
        boolean blavatarEh = !TextUtils.isEmpty(blavatarUrl);
        boolean avatarEh = !TextUtils.isEmpty(avatarUrl);

        AppLog.w(AppLog.T.READER, avatarUrl);

        int frameSize = getResources().getDimensionPixelSize(R.dimen.reader_detail_header_avatar_frame);

        View avatarFrame = findViewById(R.id.frame_avatar);
        WPNetworkImageView imgBlavatar = (WPNetworkImageView) findViewById(R.id.image_header_blavatar);
        WPNetworkImageView imgAvatar = (WPNetworkImageView) findViewById(R.id.image_header_avatar);

        /*
         *  - if there's a blavatar and an avatar, show both of them overlaid using default sizing
         *  - if there's only a blavatar, show it the full size of the parent frame and hide the avatar
         *  - if there's only an avatar, show it the full size of the parent frame and hide the blavatar
         *  - if there's neither a blavatar nor an avatar, hide them both
         */
        if (blavatarEh && avatarEh) {
            int blavatarSz = getResources().getDimensionPixelSize(R.dimen.reader_detail_header_blavatar);
            imgBlavatar.getLayoutParams().height = blavatarSz;
            imgBlavatar.getLayoutParams().width = blavatarSz;
            imgBlavatar.setImageUrl(
                    PhotonUtils.getPhotonImageUrl(blavatarUrl, blavatarSz, blavatarSz),
                    WPNetworkImageView.ImageType.BLAVATAR);
            imgBlavatar.setVisibility(View.VISIBLE);

            int avatarSz = getResources().getDimensionPixelSize(R.dimen.reader_detail_header_avatar);
            imgAvatar.getLayoutParams().height = avatarSz;
            imgAvatar.getLayoutParams().width = avatarSz;
            imgAvatar.setImageUrl(
                    GravatarUtils.fixGravatarUrl(avatarUrl, avatarSz),
                    WPNetworkImageView.ImageType.AVATAR);
            imgAvatar.setVisibility(View.VISIBLE);
        } else if (blavatarEh) {
            imgBlavatar.getLayoutParams().height = frameSize;
            imgBlavatar.getLayoutParams().width = frameSize;
            imgBlavatar.setImageUrl(
                    PhotonUtils.getPhotonImageUrl(blavatarUrl, frameSize, frameSize),
                    WPNetworkImageView.ImageType.BLAVATAR);
            imgBlavatar.setVisibility(View.VISIBLE);

            imgAvatar.setVisibility(View.GONE);
        } else if (avatarEh) {
            imgBlavatar.setVisibility(View.GONE);

            imgAvatar.getLayoutParams().height = frameSize;
            imgAvatar.getLayoutParams().width = frameSize;
            imgAvatar.setImageUrl(
                    GravatarUtils.fixGravatarUrl(avatarUrl, frameSize),
                    WPNetworkImageView.ImageType.AVATAR);
            imgAvatar.setVisibility(View.VISIBLE);
        } else {
            imgBlavatar.setVisibility(View.GONE);
            imgAvatar.setVisibility(View.GONE);
        }

        // hide the frame if there's neither a blavatar nor an avatar
        avatarFrame.setVisibility(avatarEh || blavatarEh ? View.VISIBLE : View.GONE);

        if (mEnableBlogPreview) {
            imgBlavatar.setOnClickListener(mClickListener);
            imgAvatar.setOnClickListener(mClickListener);
        }
    }

    /*
     * click listener which shows blog preview
     */
    private final OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPost != null) {
                ReaderActivityLauncher.showReaderBlogPreview(v.getContext(), mPost);
            }
        }
    };

    private void toggleFollowStatus() {
        if (!NetworkUtils.checkConnection(getContext())) return;

        final boolean askingToFollowEh = !mPost.followedByCurrentUserEh;

        ReaderActions.ActionListener listener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (getContext() == null) return;

                mFollowButton.setEnabled(true);
                if (succeeded) {
                    mPost.followedByCurrentUserEh = askingToFollowEh;
                } else {
                    int errResId = askingToFollowEh ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog;
                    ToastUtils.showToast(getContext(), errResId);
                    mFollowButton.setIsFollowed(!askingToFollowEh);
                }
            }
        };

        // disable follow button until API call returns
        mFollowButton.setEnabled(false);

        boolean result;
        if (mPost.externalEh) {
            result = ReaderBlogActions.followFeedById(mPost.feedId, askingToFollowEh, listener);
        } else {
            result = ReaderBlogActions.followBlogById(mPost.blogId, askingToFollowEh, listener);
        }

        if (result) {
            mFollowButton.setIsFollowedAnimated(askingToFollowEh);
        }
    }
}
