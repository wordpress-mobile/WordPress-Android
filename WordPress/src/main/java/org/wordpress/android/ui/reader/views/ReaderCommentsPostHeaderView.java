package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderPost;

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
    }

    public void setPost(final ReaderPost post) {
        if (post == null) {
            return;
        }

        TextView replyToAuthor = findViewById(R.id.reply_to_author);
        TextView postTitle = findViewById(R.id.post_title);

        replyToAuthor
                .setText(replyToAuthor.getContext().getString(R.string.comment_reply_to_user, post.getAuthorName()));

        postTitle.setText(post.getTitle());
    }
}
