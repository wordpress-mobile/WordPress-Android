package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderInterfaces.OnFollowListener;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;


/**
 * topmost view in post detail - shows blavatar + avatar, author name, blog name, and follow button
 */
public class ReaderPostDetailHeaderView extends LinearLayout {
    private OnFollowListener mFollowListener;
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
        mFollowButton = view.findViewById(R.id.header_follow_button);
    }

    public void setOnFollowListener(OnFollowListener listener) {
        mFollowListener = listener;
    }

    public void setPost(@NonNull ReaderPost post, boolean isSignedInWPCom) {
        mPost = post;

        TextView txtTitle = findViewById(R.id.text_header_title);
        TextView txtSubtitle = findViewById(R.id.text_header_subtitle);
        ImageView blavatar = findViewById(R.id.image_header_blavatar);

        boolean hasBlogName = mPost.hasBlogName();
        boolean hasAuthorName = mPost.hasAuthorName();

        if (hasBlogName && hasAuthorName) {
            // don't show author name if it's the same as the blog name
            if (mPost.getAuthorName().equals(mPost.getBlogName())) {
                txtTitle.setText(mPost.getAuthorName());
                txtSubtitle.setVisibility(View.GONE);
            } else {
                txtTitle.setText(mPost.getAuthorName());
                txtSubtitle.setText(mPost.getBlogName());
            }
            blavatar.setContentDescription(mPost.getBlogName());
        } else if (hasBlogName) {
            txtTitle.setText(mPost.getBlogName());
            blavatar.setContentDescription(mPost.getBlogName());
            txtSubtitle.setVisibility(View.GONE);
        } else if (hasAuthorName) {
            txtTitle.setText(mPost.getAuthorName());
            blavatar.setContentDescription(mPost.getAuthorName());
            txtSubtitle.setVisibility(View.GONE);
        } else {
            txtTitle.setText(R.string.untitled);
            txtSubtitle.setVisibility(View.GONE);
        }

        if (mEnableBlogPreview) {
            txtTitle.setOnClickListener(mClickListener);
            txtSubtitle.setOnClickListener(mClickListener);
        } else {
            int color = ContextExtensionsKt.getColorFromAttribute(getContext(), R.attr.colorOnSurface);
            txtTitle.setTextColor(color);
            txtSubtitle.setTextColor(color);
        }

        if (isSignedInWPCom) {
            mFollowButton.setVisibility(View.VISIBLE);
            mFollowButton.setIsFollowed(mPost.isFollowedByCurrentUser);
            mFollowButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowStatus(v);
                }
            });
        } else {
            mFollowButton.setVisibility(View.GONE);
        }

        showBlavatar(mPost.getBlogImageUrl(), mPost.getPostAvatar());
    }

    private void showBlavatar(String blavatarUrl, String avatarUrl) {
        ImageManager imageManager = ImageManager.getInstance();
        boolean hasBlavatar = !TextUtils.isEmpty(blavatarUrl);

        AppLog.w(AppLog.T.READER, avatarUrl);

        ImageView imgBlavatar = findViewById(R.id.image_header_blavatar);
        imageManager.cancelRequestAndClearImageView(imgBlavatar);

        if (hasBlavatar) {
            int blavatarSz = getResources().getDimensionPixelSize(R.dimen.reader_detail_header_blavatar);
            imgBlavatar.getLayoutParams().height = blavatarSz;
            imgBlavatar.getLayoutParams().width = blavatarSz;
            imageManager.load(imgBlavatar, ImageType.BLAVATAR,
                    PhotonUtils.getPhotonImageUrl(blavatarUrl, blavatarSz, blavatarSz));
            imgBlavatar.setVisibility(View.VISIBLE);
        } else {
            imgBlavatar.setVisibility(View.GONE);
        }


        if (mEnableBlogPreview) {
            imgBlavatar.setOnClickListener(mClickListener);
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

    private void toggleFollowStatus(final View followButton) {
        if (!NetworkUtils.checkConnection(getContext())) {
            return;
        }

        final boolean isAskingToFollow = !mPost.isFollowedByCurrentUser;

        if (mFollowListener != null) {
            if (isAskingToFollow) {
                mFollowListener.onFollowTapped(followButton, mPost.getBlogName(), mPost.blogId);
            } else {
                mFollowListener.onFollowingTapped();
            }
        }

        ReaderActions.ActionListener listener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (getContext() == null) {
                    return;
                }

                mFollowButton.setEnabled(true);
                if (succeeded) {
                    mPost.isFollowedByCurrentUser = isAskingToFollow;
                } else {
                    int errResId = isAskingToFollow ? R.string.reader_toast_err_follow_blog
                            : R.string.reader_toast_err_unfollow_blog;
                    ToastUtils.showToast(getContext(), errResId);
                    mFollowButton.setIsFollowed(!isAskingToFollow);
                }
            }
        };

        // disable follow button until API call returns
        mFollowButton.setEnabled(false);

        boolean result;
        if (mPost.isExternal) {
            result = ReaderBlogActions.followFeedById(mPost.feedId, isAskingToFollow, listener);
        } else {
            result = ReaderBlogActions.followBlogById(mPost.blogId, isAskingToFollow, listener);
        }

        if (result) {
            mFollowButton.setIsFollowedAnimated(isAskingToFollow);
        }
    }
}
