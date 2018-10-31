package org.wordpress.android.ui.notifications.blocks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.tools.FormattableContent;
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper;
import org.wordpress.android.util.FormattableContentUtilsKt;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.List;

// Note header, displayed at top of detail view
public class HeaderNoteBlock extends NoteBlock {
    private final List<FormattableContent> mHeadersList;

    private final UserNoteBlock.OnGravatarClickedListener mGravatarClickedListener;
    private Boolean mIsComment;
    private int mAvatarSize;

    private ImageType mImageType;

    public HeaderNoteBlock(Context context, List<FormattableContent> headerArray, ImageType imageType,
                           OnNoteBlockTextClickListener onNoteBlockTextClickListener,
                           UserNoteBlock.OnGravatarClickedListener onGravatarClickedListener,
                           ImageManager imageManager, NotificationsUtilsWrapper notificationsUtilsWrapper) {
        super(new FormattableContent(), imageManager, notificationsUtilsWrapper, onNoteBlockTextClickListener);
        mHeadersList = headerArray;
        mImageType = imageType;
        mGravatarClickedListener = onGravatarClickedListener;

        if (context != null) {
            mAvatarSize = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
        }
    }

    @Override
    public BlockType getBlockType() {
        return BlockType.USER_HEADER;
    }

    public int getLayoutResourceId() {
        return R.layout.note_block_header;
    }

    @SuppressLint("ClickableViewAccessibility") // fixed by setting a click listener to avatarImageView
    @Override
    public View configureView(View view) {
        final NoteHeaderBlockHolder noteBlockHolder = (NoteHeaderBlockHolder) view.getTag();

        Spannable spannable = mNotificationsUtilsWrapper.getSpannableContentForRanges(mHeadersList.get(0));
        noteBlockHolder.mNameTextView.setText(spannable);
        if (mImageType == ImageType.AVATAR_WITH_BACKGROUND) {
            mImageManager.loadIntoCircle(noteBlockHolder.mAvatarImageView, mImageType, getAvatarUrl());
        } else {
            mImageManager.load(noteBlockHolder.mAvatarImageView, mImageType, getAvatarUrl());
        }

        final long siteId = FormattableContentUtilsKt.getRangeSiteIdOrZero(getHeader(0), 0);
        final long userId = FormattableContentUtilsKt.getRangeIdOrZero(getHeader(0), 0);

        if (!TextUtils.isEmpty(getUserUrl()) && siteId > 0 && userId > 0) {
            noteBlockHolder.mAvatarImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String siteUrl = getUserUrl();
                    mGravatarClickedListener.onGravatarClicked(siteId, userId, siteUrl);
                }
            });


            noteBlockHolder.mAvatarImageView.setContentDescription(
                    view.getContext().getString(R.string.profile_picture, spannable));
            //noinspection AndroidLintClickableViewAccessibility
            noteBlockHolder.mAvatarImageView.setOnTouchListener(mOnGravatarTouchListener);

            if (siteId == userId) {
                noteBlockHolder.mAvatarImageView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            } else {
                noteBlockHolder.mAvatarImageView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }
        } else {
            noteBlockHolder.mAvatarImageView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            noteBlockHolder.mAvatarImageView.setContentDescription(null);
            noteBlockHolder.mAvatarImageView.setOnClickListener(null);
            //noinspection AndroidLintClickableViewAccessibility
            noteBlockHolder.mAvatarImageView.setOnTouchListener(null);
        }

        noteBlockHolder.mSnippetTextView.setText(getSnippet());

        if (mIsComment) {
            View footerView = view.findViewById(R.id.header_footer);
            View footerCommentView = view.findViewById(R.id.header_footer_comment);
            footerView.setVisibility(View.GONE);
            footerCommentView.setVisibility(View.VISIBLE);
        }

        return view;
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (getOnNoteBlockTextClickListener() != null) {
                getOnNoteBlockTextClickListener().showDetailForNoteIds();
            }
        }
    };

    private String getAvatarUrl() {
        return GravatarUtils.fixGravatarUrl(FormattableContentUtilsKt.getMediaUrlOrEmpty(getHeader(0), 0), mAvatarSize);
    }

    private String getUserUrl() {
        return FormattableContentUtilsKt.getRangeUrlOrEmpty(getHeader(0), 0);
    }

    private String getSnippet() {
        return FormattableContentUtilsKt.getTextOrEmpty(getHeader(1));
    }

    @Override
    public Object getViewHolder(View view) {
        return new NoteHeaderBlockHolder(view);
    }

    public void setIsComment(Boolean isComment) {
        mIsComment = isComment;
    }

    private class NoteHeaderBlockHolder {
        private final TextView mNameTextView;
        private final TextView mSnippetTextView;
        private final ImageView mAvatarImageView;

        NoteHeaderBlockHolder(View view) {
            View rootView = view.findViewById(R.id.header_root_view);
            rootView.setOnClickListener(mOnClickListener);
            mNameTextView = view.findViewById(R.id.header_user);
            mSnippetTextView = view.findViewById(R.id.header_snippet);
            mAvatarImageView = view.findViewById(R.id.header_avatar);
        }
    }

    private final View.OnTouchListener mOnGravatarTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int animationDuration = 150;

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate()
                 .scaleX(0.9f)
                 .scaleY(0.9f)
                 .alpha(0.5f)
                 .setDuration(animationDuration)
                 .setInterpolator(new DecelerateInterpolator());
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                       || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                v.animate()
                 .scaleX(1.0f)
                 .scaleY(1.0f)
                 .alpha(1.0f)
                 .setDuration(animationDuration)
                 .setInterpolator(new DecelerateInterpolator());

                if (event.getActionMasked() == MotionEvent.ACTION_UP && mGravatarClickedListener != null) {
                    // Fire the listener, which will load the site preview for the user's site
                    // In the future we can use this to load a 'profile view' (currently in R&D)
                    v.performClick();
                }
            }

            return true;
        }
    };

    private FormattableContent getHeader(int headerIndex) {
        if (mHeadersList != null && headerIndex < mHeadersList.size()) {
            return mHeadersList.get(headerIndex);
        }
        return null;
    }
}
