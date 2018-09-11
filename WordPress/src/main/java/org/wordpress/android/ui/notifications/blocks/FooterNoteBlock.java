package org.wordpress.android.ui.notifications.blocks;

import android.text.Spannable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.tools.FormattableContent;
import org.wordpress.android.fluxc.tools.FormattableRange;
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper;
import org.wordpress.android.util.FormattableContentUtilsKt;
import org.wordpress.android.util.RtlUtils;
import org.wordpress.android.util.image.ImageManager;

public class FooterNoteBlock extends NoteBlock {
    private NoteBlockClickableSpan mClickableSpan;

    public FooterNoteBlock(FormattableContent noteObject,
                           ImageManager imageManager,
                           NotificationsUtilsWrapper notificationsUtilsWrapper,
                           OnNoteBlockTextClickListener onNoteBlockTextClickListener) {
        super(noteObject, imageManager, notificationsUtilsWrapper, onNoteBlockTextClickListener);
    }

    public void setClickableSpan(FormattableRange rangeObject, String noteType) {
        if (rangeObject == null) {
            return;
        }

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
        final FooterNoteBlockHolder noteBlockHolder = (FooterNoteBlockHolder) view.getTag();

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
            // mirror noticon in the rtl mode
            if (RtlUtils.isRtl(noteBlockHolder.getNoticonView().getContext())) {
                noteBlockHolder.getNoticonView().setScaleX(-1);
            }
        } else {
            noteBlockHolder.getNoticonView().setVisibility(View.GONE);
        }

        return view;
    }

    @NotNull
    private String getNoticonGlyph() {
        return FormattableContentUtilsKt.getRangeValueOrEmpty(getNoteData(), 0);
    }

    @Override
    Spannable getNoteText() {
        return mNotificationsUtilsWrapper.getSpannableContentForRanges(getNoteData(), null,
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
            mTextView = view.findViewById(R.id.note_footer_text);
            mNoticonView = view.findViewById(R.id.note_footer_noticon);
        }

        public TextView getTextView() {
            return mTextView;
        }

        public TextView getNoticonView() {
            return mNoticonView;
        }
    }

    private void onRangeClick() {
        if (mClickableSpan == null || getOnNoteBlockTextClickListener() == null) {
            return;
        }

        getOnNoteBlockTextClickListener().onNoteBlockTextClicked(mClickableSpan);
    }
}
