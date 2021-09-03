package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.facebook.shimmer.ShimmerFrameLayout;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.FollowCommentsUiState;
import org.wordpress.android.ui.reader.FollowCommentsUiStateType;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

/**
 * topmost view in reader comment adapter - show info about the post
 */
public class ReaderCommentsPostHeaderView extends LinearLayout {
    public ReaderCommentsPostHeaderView(Context context) {
        super(context);
        initView(context);
    }

    public ReaderCommentsPostHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderCommentsPostHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        ((WordPress) context.getApplicationContext()).component().inject(this);
        inflate(context, R.layout.reader_comments_post_header_view, this);

        setBackgroundColor(ColorUtils
                .setAlphaComponent(ContextExtensionsKt.getColorFromAttribute(getContext(), R.attr.colorOnSurface),
                        getResources().getInteger(R.integer.selected_list_item_opacity)));
    }

    public void setPost(
            final ReaderPost post,
            final FollowCommentsUiState followButtonState
    ) {
        if (post == null) {
            return;
        }

        TextView txtTitle = findViewById(R.id.text_post_title);
        TextView txtBlogName = findViewById(R.id.text_blog_name);
        TextView txtDateline = findViewById(R.id.text_post_dateline);
        ImageView imgAvatar = findViewById(R.id.image_post_avatar);

        txtTitle.setText(post.getTitle());
        if (post.hasBlogName()) {
            txtBlogName.setText(post.getBlogName());
        } else {
            txtBlogName.setText(R.string.reader_untitled_post);
        }

        java.util.Date dtPost = DateTimeUtils.dateFromIso8601(post.getDatePublished());
        String dateLine = DateTimeUtils.javaDateToTimeSpan(dtPost, WordPress.getContext());
        if (post.isCommentsOpen || post.numReplies > 0) {
            dateLine += " \u2022 " + ReaderUtils.getShortCommentLabelText(getContext(), post.numReplies);
        }
        if (post.canLikePost() || post.numLikes > 0) {
            dateLine += " \u2022 " + ReaderUtils.getShortLikeLabelText(getContext(), post.numLikes);
        }
        txtDateline.setText(dateLine);

        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_extra_small);
        String avatarUrl;
        if (post.hasBlogUrl()) {
            avatarUrl = GravatarUtils.blavatarFromUrl(post.getBlogUrl(), avatarSz);
            ImageManager.getInstance().load(imgAvatar, ImageType.BLAVATAR, avatarUrl);
        } else {
            avatarUrl = post.getPostAvatarForDisplay(avatarSz);
            ImageManager.getInstance().loadIntoCircle(imgAvatar, ImageType.AVATAR, avatarUrl);
        }

        setFollowButtonState(followButtonState);
    }

    public void setFollowButtonState(@Nullable final FollowCommentsUiState followButtonState) {
        if (null == followButtonState) return;

        ReaderFollowButton followCommentsButton = findViewById(R.id.button_follow_comments);
        ShimmerFrameLayout container = findViewById(R.id.shimmer_view_container);
        View skeleton = findViewById(R.id.button_skeleton);

        boolean isButtonEnabled = followButtonState.getType() != FollowCommentsUiStateType.DISABLED
                                  && followButtonState.getType() != FollowCommentsUiStateType.LOADING;

        followCommentsButton.setEnabled(isButtonEnabled);

        boolean isContainerVisible = container.getVisibility() == View.VISIBLE;
        if (isContainerVisible != followButtonState.getShowFollowButton()) {
            container.setVisibility(followButtonState.getShowFollowButton() ? View.VISIBLE : View.GONE);
        }

        if (followButtonState.getType() == FollowCommentsUiStateType.LOADING) {
            if (skeleton.getVisibility() != View.VISIBLE) {
                skeleton.setVisibility(View.VISIBLE);
                followCommentsButton.setVisibility(View.GONE);
                container.showShimmer(true);
            }
        } else {
            skeleton.setVisibility(View.GONE);
            followCommentsButton.setVisibility(View.VISIBLE);
            container.hideShimmer();
        }

        if (followButtonState.getType() == FollowCommentsUiStateType.VISIBLE_WITH_STATE) {
            if (followButtonState.getAnimate()) {
                followCommentsButton.setIsFollowedAnimated(followButtonState.isFollowing());
            } else {
                followCommentsButton.setIsFollowed(followButtonState.isFollowing());
            }
        }

        if (followButtonState.getOnFollowButtonClick() != null) {
            followCommentsButton.setOnClickListener(
                    v -> followButtonState.getOnFollowButtonClick().invoke(!followButtonState.isFollowing()));
        } else {
            followCommentsButton.setOnClickListener(null);
        }
    }
}
