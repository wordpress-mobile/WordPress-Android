package org.wordpress.android.ui.notifications.blocks;

import android.text.Spannable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.JSONUtils;

public class FooterNoteBlock extends NoteBlock {
    private NoteBlockClickableSpan mClickableSpan;

    public FooterNoteBlock(JSONObject noteObject, OnNoteBlockTextClickListener onNoteBlockTextClickListener) {
        super(noteObject, onNoteBlockTextClickListener);
    }

    public void setClickableSpan(JSONObject rangeObject, String noteType) {
        if (rangeObject == null) return;

        mClickableSpan = new NoteBlockClickableSpan(
                WordPress.getContext(),
                rangeObject,
                false,
                true
        );

        mClickableSpan.setCustomType(noteType);
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
    public View configureView(final View view) {
        final FooterNoteBlockHolder noteBlockHolder = (FooterNoteBlockHolder)view.getTag();

        // Note text
        if (!TextUtils.isEmpty(getNoteText())) {
            noteBlockHolder.getTextView().setText(getNoteText());
            noteBlockHolder.getTextView().setVisibility(View.VISIBLE);
        } else {
            noteBlockHolder.getTextView().setVisibility(View.GONE);
        }

        String noticonGlyph = getNoticonGlyph();
        if (!TextUtils.isEmpty(noticonGlyph)) {
            noteBlockHolder.getNoticonView().setVisibility(View.VISIBLE);
            noteBlockHolder.getNoticonView().setText(noticonGlyph);
        } else {
            noteBlockHolder.getNoticonView().setVisibility(View.GONE);
        }

        return view;
    }

    private String getNoticonGlyph() {
        if (getNoteData() == null) return "";

        return JSONUtils.queryJSON(getNoteData(), "ranges[first].value", "");
    }

    @Override
    public Spannable getNoteText() {
        return NotificationsUtils.getSpannableContentForRanges(getNoteData(), null,
                getOnNoteBlockTextClickListener(), true);
    }

    public Object getViewHolder(View view) {
        return new FooterNoteBlockHolder(view);
    }

    class FooterNoteBlockHolder {
        private final View mFooterView;
        private final TextView mTextView;
        private final TextView mNoticonView;

        FooterNoteBlockHolder(View view) {
            mFooterView = view.findViewById(R.id.note_footer);
            mFooterView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRangeClick();
                }
            });
            mTextView = (TextView) view.findViewById(R.id.note_footer_text);
            mNoticonView = (TextView) view.findViewById(R.id.note_footer_noticon);
        }

        public TextView getTextView() {
            return mTextView;
        }
        public TextView getNoticonView() {
            return mNoticonView;
        }
    }

    public void onRangeClick() {
        if (mClickableSpan == null || getOnNoteBlockTextClickListener() == null) {
            return;
        }

        getOnNoteBlockTextClickListener().onNoteBlockTextClicked(mClickableSpan);
    }

}
