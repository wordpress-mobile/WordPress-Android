package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

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
    }

    public void setPost(final ReaderPost post) {
        if (post == null) return;

        TextView txtTitle = (TextView) findViewById(R.id.text_post_title);
        TextView txtBlogName = (TextView) findViewById(R.id.text_blog_name);
        TextView txtDateline = (TextView) findViewById(R.id.text_post_dateline);
        WPNetworkImageView imgAvatar = (WPNetworkImageView) findViewById(R.id.image_post_avatar);

        txtTitle.setText(post.getTitle());
        if (post.hasBlogName()) {
            txtBlogName.setText(post.getBlogName());
        } else {
            txtBlogName.setText(R.string.reader_untitled_post);
        }

        java.util.Date dtPublished = DateTimeUtils.iso8601ToJavaDate(post.getPublished());
        String dateLine = DateTimeUtils.javaDateToTimeSpan(dtPublished);
        if (post.isCommentsOpen || post.numReplies > 0) {
            dateLine += "  \u2022  " + ReaderUtils.getShortCommentLabelText(getContext(), post.numReplies);
        }
        if (post.canLikePost() || post.numLikes > 0) {
            dateLine += "  \u2022  " + ReaderUtils.getShortLikeLabelText(getContext(), post.numLikes);
        }
        txtDateline.setText(dateLine);

        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_extra_small);
        String avatarUrl;
        if (post.hasBlogUrl()) {
            avatarUrl = GravatarUtils.blavatarFromUrl(post.getBlogUrl(), avatarSz);
            imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.BLAVATAR);
        } else {
            avatarUrl = post.getPostAvatarForDisplay(avatarSz);
            imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
        }
    }
}
