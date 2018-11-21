package org.wordpress.android.ui.notifications.blocks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.tools.FormattableContent;
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

// A user block with slightly different formatting for display in a comment detail
public class CommentUserNoteBlock extends UserNoteBlock {
    private static final String EMPTY_LINE = "\n\t";
    private static final String DOUBLE_EMPTY_LINE = "\n\t\n\t";
    private CommentStatus mCommentStatus = CommentStatus.APPROVED;
    private int mNormalBackgroundColor;
    private int mNormalTextColor;
    private int mAgoTextColor;
    private int mUnapprovedTextColor;
    private int mIndentedLeftPadding;

    private boolean mStatusChanged;

    private final FormattableContent mCommentData;
    private final long mTimestamp;

    public interface OnCommentStatusChangeListener {
        void onCommentStatusChanged(CommentStatus newStatus);
    }

    public CommentUserNoteBlock(Context context, FormattableContent noteObject,
                                FormattableContent commentTextBlock,
                                long timestamp, OnNoteBlockTextClickListener onNoteBlockTextClickListener,
                                OnGravatarClickedListener onGravatarClickedListener,
                                ImageManager imageManager, NotificationsUtilsWrapper notificationsUtilsWrapper) {
        super(context, noteObject, onNoteBlockTextClickListener, onGravatarClickedListener, imageManager,
                notificationsUtilsWrapper);

        mCommentData = commentTextBlock;
        mTimestamp = timestamp;

        if (context != null) {
            setAvatarSize(context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small));
        }
    }

    @Override
    public BlockType getBlockType() {
        return BlockType.USER_COMMENT;
    }

    @Override
    public int getLayoutResourceId() {
        return R.layout.note_block_comment_user;
    }

    @SuppressLint("ClickableViewAccessibility") // fixed by setting a click listener to avatarImageView
    @Override
    public View configureView(View view) {
        final CommentUserNoteBlockHolder noteBlockHolder = (CommentUserNoteBlockHolder) view.getTag();

        noteBlockHolder.mNameTextView
                .setText(Html.fromHtml("<strong>" + getNoteText().toString() + "</strong>"));
        noteBlockHolder.mAgoTextView.setText(DateTimeUtils.timeSpanFromTimestamp(getTimestamp(),
                WordPress.getContext()));
        if (!TextUtils.isEmpty(getMetaHomeTitle()) || !TextUtils.isEmpty(getMetaSiteUrl())) {
            noteBlockHolder.mBulletTextView.setVisibility(View.VISIBLE);
            noteBlockHolder.mSiteTextView.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(getMetaHomeTitle())) {
                noteBlockHolder.mSiteTextView.setText(getMetaHomeTitle());
            } else {
                noteBlockHolder.mSiteTextView.setText(getMetaSiteUrl().replace("http://", "").replace("https://", ""));
            }
        } else {
            noteBlockHolder.mBulletTextView.setVisibility(View.GONE);
            noteBlockHolder.mSiteTextView.setVisibility(View.GONE);
        }

        noteBlockHolder.mSiteTextView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        String imageUrl = "";
        if (hasImageMediaItem()) {
            imageUrl = GravatarUtils.fixGravatarUrl(getNoteMediaItem().getUrl(), getAvatarSize());
            noteBlockHolder.mAvatarImageView.setContentDescription(
                    view.getContext()
                        .getString(R.string.profile_picture, getNoteText().toString()));
            if (!TextUtils.isEmpty(getUserUrl())) {
                noteBlockHolder.mAvatarImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showBlogPreview();
                    }
                });
                //noinspection AndroidLintClickableViewAccessibility
                noteBlockHolder.mAvatarImageView.setOnTouchListener(mOnGravatarTouchListener);
            } else {
                noteBlockHolder.mAvatarImageView.setOnClickListener(null);
                //noinspection AndroidLintClickableViewAccessibility
                noteBlockHolder.mAvatarImageView.setOnTouchListener(null);
                noteBlockHolder.mAvatarImageView.setContentDescription(null);
            }
        } else {
            noteBlockHolder.mAvatarImageView.setOnClickListener(null);
            //noinspection AndroidLintClickableViewAccessibility
            noteBlockHolder.mAvatarImageView.setOnTouchListener(null);
            noteBlockHolder.mAvatarImageView.setContentDescription(null);
        }
        mImageManager.loadIntoCircle(noteBlockHolder.mAvatarImageView, ImageType.AVATAR_WITH_BACKGROUND, imageUrl);

        noteBlockHolder.mCommentTextView
                .setText(getCommentTextOfNotification(noteBlockHolder));

        // Change display based on comment status and type:
        // 1. Comment replies are indented and have a 'pipe' background
        // 2. Unapproved comments have different background and text color
        int paddingStart = ViewCompat.getPaddingStart(view);
        int paddingTop = view.getPaddingTop();
        int paddingEnd = ViewCompat.getPaddingEnd(view);
        int paddingBottom = view.getPaddingBottom();
        if (mCommentStatus == CommentStatus.UNAPPROVED) {
            if (hasCommentNestingLevel()) {
                paddingStart = mIndentedLeftPadding;
                view.setBackgroundResource(R.drawable.comment_reply_unapproved_background);
            } else {
                view.setBackgroundResource(R.drawable.comment_unapproved_background);
            }

            noteBlockHolder.mDividerView.setVisibility(View.INVISIBLE);

            noteBlockHolder.mAgoTextView.setTextColor(mUnapprovedTextColor);
            noteBlockHolder.mBulletTextView.setTextColor(mUnapprovedTextColor);
            noteBlockHolder.mSiteTextView.setTextColor(mUnapprovedTextColor);
            noteBlockHolder.mNameTextView.setTextColor(mUnapprovedTextColor);
            noteBlockHolder.mCommentTextView.setTextColor(mUnapprovedTextColor);
        } else {
            if (hasCommentNestingLevel()) {
                paddingStart = mIndentedLeftPadding;
                view.setBackgroundResource(R.drawable.comment_reply_background);
                noteBlockHolder.mDividerView.setVisibility(View.INVISIBLE);
            } else {
                view.setBackgroundColor(mNormalBackgroundColor);
                noteBlockHolder.mDividerView.setVisibility(View.VISIBLE);
            }

            noteBlockHolder.mAgoTextView.setTextColor(mAgoTextColor);
            noteBlockHolder.mBulletTextView.setTextColor(mAgoTextColor);
            noteBlockHolder.mSiteTextView.setTextColor(mAgoTextColor);
            noteBlockHolder.mNameTextView.setTextColor(mNormalTextColor);
            noteBlockHolder.mCommentTextView.setTextColor(mNormalTextColor);
        }
        ViewCompat.setPaddingRelative(view, paddingStart, paddingTop, paddingEnd, paddingBottom);

        // If status was changed, fade in the view
        if (mStatusChanged) {
            mStatusChanged = false;
            view.setAlpha(0.4f);
            view.animate().alpha(1.0f).start();
        }

        return view;
    }

    private Spannable getCommentTextOfNotification(CommentUserNoteBlockHolder noteBlockHolder) {
        SpannableStringBuilder builder = mNotificationsUtilsWrapper
                .getSpannableContentForRanges(mCommentData,
                        noteBlockHolder.mCommentTextView, getOnNoteBlockTextClickListener(), false);
        return removeNewLineInList(builder);
    }

    private Spannable removeNewLineInList(SpannableStringBuilder builder) {
        String content = builder.toString();
        while (content.contains(DOUBLE_EMPTY_LINE)) {
            int doubleSpaceIndex = content.indexOf(DOUBLE_EMPTY_LINE);
            builder.replace(doubleSpaceIndex, doubleSpaceIndex + DOUBLE_EMPTY_LINE.length(), EMPTY_LINE);
            content = builder.toString();
        }
        return builder;
    }

    private long getTimestamp() {
        return mTimestamp;
    }

    private boolean hasCommentNestingLevel() {
        return mCommentData.getNestLevel() != null && mCommentData.getNestLevel() > 0;
    }

    @Override
    public Object getViewHolder(View view) {
        return new CommentUserNoteBlockHolder(view);
    }

    private class CommentUserNoteBlockHolder {
        private final ImageView mAvatarImageView;
        private final TextView mNameTextView;
        private final TextView mAgoTextView;
        private final TextView mBulletTextView;
        private final TextView mSiteTextView;
        private final TextView mCommentTextView;
        private final View mDividerView;

        CommentUserNoteBlockHolder(View view) {
            mNameTextView = view.findViewById(R.id.user_name);
            mAgoTextView = view.findViewById(R.id.user_comment_ago);
            mAgoTextView.setVisibility(View.VISIBLE);
            mBulletTextView = view.findViewById(R.id.user_comment_bullet);
            mSiteTextView = view.findViewById(R.id.user_comment_site);
            mCommentTextView = view.findViewById(R.id.user_comment);
            mCommentTextView.setMovementMethod(new NoteBlockLinkMovementMethod());
            mAvatarImageView = view.findViewById(R.id.user_avatar);
            mDividerView = view.findViewById(R.id.divider_view);

            mSiteTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getOnNoteBlockTextClickListener() != null) {
                        getOnNoteBlockTextClickListener().showSitePreview(getMetaSiteId(), getMetaSiteUrl());
                    }
                }
            });

            // show all comments on this post when user clicks the comment text
            mCommentTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getOnNoteBlockTextClickListener() != null) {
                        getOnNoteBlockTextClickListener().showReaderPostComments();
                    }
                }
            });
        }
    }

    public void configureResources(Context context) {
        if (context == null) {
            return;
        }

        mNormalTextColor = context.getResources().getColor(R.color.grey_dark);
        mNormalBackgroundColor = context.getResources().getColor(R.color.white);
        mAgoTextColor = context.getResources().getColor(R.color.grey_text_min);
        mUnapprovedTextColor = context.getResources().getColor(R.color.notification_status_unapproved_dark);
        // Double margin_extra_large for increased indent in comment replies
        mIndentedLeftPadding = context.getResources().getDimensionPixelSize(R.dimen.margin_extra_large) * 2;
    }

    private final OnCommentStatusChangeListener mOnCommentChangedListener = new OnCommentStatusChangeListener() {
        @Override
        public void onCommentStatusChanged(CommentStatus newStatus) {
            mCommentStatus = newStatus;
            mStatusChanged = true;
        }
    };

    public void setCommentStatus(CommentStatus status) {
        mCommentStatus = status;
    }

    public OnCommentStatusChangeListener getOnCommentChangeListener() {
        return mOnCommentChangedListener;
    }
}
