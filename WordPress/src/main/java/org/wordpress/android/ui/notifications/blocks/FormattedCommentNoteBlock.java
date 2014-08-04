package org.wordpress.android.ui.notifications.blocks;

import org.json.JSONObject;
import org.wordpress.android.R;

/**
 * A block that displays a parent comment to a comment reply notification
 * Very similar to a NoteBlock, but with different styling in the layout xml
 */
public class FormattedCommentNoteBlock extends NoteBlock {

    public FormattedCommentNoteBlock(JSONObject noteObject, OnNoteBlockTextClickListener onNoteBlockTextClickListener) {
        super(noteObject, onNoteBlockTextClickListener);
    }

    @Override
    public int getLayoutResourceId() {
        return R.layout.note_block_formatted_comment;
    }


}
