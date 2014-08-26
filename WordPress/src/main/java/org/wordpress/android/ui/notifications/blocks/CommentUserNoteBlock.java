package org.wordpress.android.ui.notifications.blocks;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.comments.CommentUtils;
import org.wordpress.android.ui.notifications.utils.NotificationUtils;

// A user block with slightly different formatting for display in a comment detail
public class CommentUserNoteBlock extends UserNoteBlock {

    private int mTextViewIndent;

    public CommentUserNoteBlock(JSONObject noteObject, OnNoteBlockTextClickListener onNoteBlockTextClickListener, OnGravatarClickedListener onGravatarClickedListener) {
        super(noteObject, onNoteBlockTextClickListener, onGravatarClickedListener);
    }

    @Override
    public BlockType getBlockType() {
        return BlockType.USER_COMMENT;
    }

    @Override
    public int getLayoutResourceId() {
        return R.layout.note_block_comment_user;
    }

    @Override
    public View configureView(View view) {
        final CommentUserNoteBlockHolder noteBlockHolder = (CommentUserNoteBlockHolder)view.getTag();
        noteBlockHolder.nameTextView.setText(getNoteText().toString());

        if (hasImageMediaItem()) {
            noteBlockHolder.avatarImageView.setImageUrl(getNoteMediaItem().optString("url", ""), WordPress.imageLoader);
            if (!TextUtils.isEmpty(getUserUrl())) {
                noteBlockHolder.avatarImageView.setOnTouchListener(mOnGravatarTouchListener);
            } else {
                noteBlockHolder.avatarImageView.setOnTouchListener(null);
            }
        } else {
            noteBlockHolder.avatarImageView.setImageResource(R.drawable.placeholder);
            noteBlockHolder.avatarImageView.setOnTouchListener(null);
        }

        if (mTextViewIndent == 0) {
            Context context = view.getContext();
            if (context != null) {
                mTextViewIndent = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small) +
                        context.getResources().getDimensionPixelSize(R.dimen.margin_medium);
            }
        }

        CommentUtils.indentTextViewFirstLine(
                noteBlockHolder.commentTextView,
                NotificationUtils.getSpannableTextFromIndices(getNoteData().optJSONObject("comment-text"), getOnNoteBlockTextClickListener()),
                mTextViewIndent
        );

        return view;
    }

    @Override
    public Object getViewHolder(View view) {
        return new CommentUserNoteBlockHolder(view);
    }

    private class CommentUserNoteBlockHolder {
        private TextView nameTextView;
        private TextView commentTextView;
        private NetworkImageView avatarImageView;

        public CommentUserNoteBlockHolder(View view) {
            nameTextView = (TextView)view.findViewById(R.id.user_name);
            commentTextView = (TextView)view.findViewById(R.id.user_comment);
            commentTextView.setMovementMethod(new NoteBlockLinkMovementMethod());
            avatarImageView = (NetworkImageView)view.findViewById(R.id.user_avatar);
        }
    }

}
