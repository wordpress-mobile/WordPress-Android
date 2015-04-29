package org.wordpress.android.ui.notifications.blocks;

import android.text.Spannable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.widgets.WPTextView;

// Note header, displayed at top of detail view
public class FooterNoteBlock extends NoteBlock {
    private NoteBlockClickableSpan mClickableSpan;

    public FooterNoteBlock(JSONObject noteObject, OnNoteBlockTextClickListener onNoteBlockTextClickListener) {
        super(noteObject, onNoteBlockTextClickListener);
    }

    public void setClickableSpan(JSONObject rangeObject) {
        if (rangeObject == null) return;

        mClickableSpan = new NoteBlockClickableSpan(
                WordPress.getContext(),
                rangeObject,
                false
        );
    }

    @Override
    public BlockType getBlockType() {
        return BlockType.FOOTER;
    }

    @Override
    public int getLayoutResourceId() {
        return R.layout.note_block_footer;
    }

    @Override
    public Spannable getNoteText() {
        return NotificationsUtils.getSpannableContentForRanges(getNoteData(), null, getOnNoteBlockTextClickListener());
    }

    @Override
    public View configureView(final View view) {
        final FooterNoteBlockHolder noteBlockHolder = (FooterNoteBlockHolder)view.getTag();

        // Note text
        if (!TextUtils.isEmpty(getNoteText())) {
            noteBlockHolder.getTextView().setText(getNoteText());
            noteBlockHolder.getTextView().setVisibility(View.VISIBLE);
        }

        return view;
    }

    public Object getViewHolder(View view) {
        return new FooterNoteBlockHolder(view);
    }

    class FooterNoteBlockHolder {
        private final View mFooterView;
        private final TextView mTextView;

        FooterNoteBlockHolder(View view) {
            mFooterView = view.findViewById(R.id.note_footer);
            mFooterView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRangeClick();
                }
            });
            mTextView = (WPTextView) view.findViewById(R.id.note_text);
        }

        public TextView getTextView() {
            return mTextView;
        }
    }

    public void onRangeClick() {
        if (mClickableSpan == null || getOnNoteBlockTextClickListener() == null) {
            return;
        }

        getOnNoteBlockTextClickListener().onNoteBlockTextClicked(mClickableSpan);
    }

}
