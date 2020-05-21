package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderPost;
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
        inflate(context, R.layout.reader_comments_post_header_view, this);

        setBackgroundColor(ColorUtils
                .setAlphaComponent(ContextExtensionsKt.getColorFromAttribute(getContext(), R.attr.colorOnSurface),
                        getResources().getInteger(R.integer.selected_list_item_opacity)));
    }

    public void setPost(final ReaderPost post) {
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
    }
}
