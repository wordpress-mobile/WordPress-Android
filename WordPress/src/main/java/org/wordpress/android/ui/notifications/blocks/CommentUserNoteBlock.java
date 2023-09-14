package org.wordpress.android.ui.notifications.blocks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.tools.FormattableContent;
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper;
import org.wordpress.android.util.extensions.ContextExtensionsKt;
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
    private int mIndentedLeftPadding;
    private final Context mContext;
    private boolean mStatusChanged;

    private FormattableContent mCommentData;
    private final long mTimestamp;

    private CommentUserNoteBlockHolder mNoteBlockHolder;

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
        mContext = context;
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
        mNoteBlockHolder = (CommentUserNoteBlockHolder) view.getTag();

        setUserName();
        setUserCommentAgo();
        setUserCommentSite();
        setUserAvatar();
        setUserComment();
        setCommentStatus(view);

        return view;
    }

    private void setUserName() {
        mNoteBlockHolder.mNameTextView.setText(
                HtmlCompat.fromHtml(
                        "<strong>" + getNoteText().toString() + "</strong>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                )
        );
    }

    private void setUserCommentAgo() {
        mNoteBlockHolder.mAgoTextView.setText(DateTimeUtils.timeSpanFromTimestamp(getTimestamp(),
                mNoteBlockHolder.mAgoTextView.getContext()));
    }

    private void setUserCommentSite() {
        if (!TextUtils.isEmpty(getMetaHomeTitle()) || !TextUtils.isEmpty(getMetaSiteUrl())) {
            mNoteBlockHolder.mBulletTextView.setVisibility(View.VISIBLE);
            mNoteBlockHolder.mSiteTextView.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(getMetaHomeTitle())) {
                mNoteBlockHolder.mSiteTextView.setText(getMetaHomeTitle());
            } else {
                mNoteBlockHolder.mSiteTextView.setText(getMetaSiteUrl().replace("http://", "").replace("https://", ""));
            }
        } else {
            mNoteBlockHolder.mBulletTextView.setVisibility(View.GONE);
            mNoteBlockHolder.mSiteTextView.setVisibility(View.GONE);
        }
        mNoteBlockHolder.mSiteTextView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private void setUserAvatar() {
        String imageUrl = "";
        if (hasImageMediaItem()) {
            imageUrl = GravatarUtils.fixGravatarUrl(getNoteMediaItem().getUrl(), getAvatarSize());
            mNoteBlockHolder.mAvatarImageView.setContentDescription(
                    mContext.getString(R.string.profile_picture, getNoteText().toString())
            );
            if (!TextUtils.isEmpty(getUserUrl())) {
                mNoteBlockHolder.mAvatarImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showBlogPreview();
                    }
                });
                //noinspection AndroidLintClickableViewAccessibility
                mNoteBlockHolder.mAvatarImageView.setOnTouchListener(mOnGravatarTouchListener);
            } else {
                mNoteBlockHolder.mAvatarImageView.setOnClickListener(null);
                //noinspection AndroidLintClickableViewAccessibility
                mNoteBlockHolder.mAvatarImageView.setOnTouchListener(null);
                mNoteBlockHolder.mAvatarImageView.setContentDescription(null);
            }
        } else {
            mNoteBlockHolder.mAvatarImageView.setOnClickListener(null);
            //noinspection AndroidLintClickableViewAccessibility
            mNoteBlockHolder.mAvatarImageView.setOnTouchListener(null);
            mNoteBlockHolder.mAvatarImageView.setContentDescription(null);
        }
        mImageManager.loadIntoCircle(mNoteBlockHolder.mAvatarImageView, ImageType.AVATAR_WITH_BACKGROUND, imageUrl);
    }

    private void setUserComment() {
        Spannable spannable = getCommentTextOfNotification(mNoteBlockHolder);
        NoteBlockClickableSpan[] spans = spannable.getSpans(0, spannable.length(), NoteBlockClickableSpan.class);
        for (NoteBlockClickableSpan span : spans) {
            span.enableColors(mContext);
        }

        mNoteBlockHolder.mCommentTextView.setText(spannable);
    }

    private void setCommentStatus(@NonNull final View view) {
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
                view.setBackgroundResource(R.drawable.bg_rectangle_warning_surface_with_padding);
            } else {
                view.setBackgroundResource(R.drawable.bg_rectangle_warning_surface);
            }

            mNoteBlockHolder.mDividerView.setVisibility(View.INVISIBLE);
        } else {
            if (hasCommentNestingLevel()) {
                paddingStart = mIndentedLeftPadding;
                view.setBackgroundResource(R.drawable.comment_reply_background);
                mNoteBlockHolder.mDividerView.setVisibility(View.INVISIBLE);
            } else {
                view.setBackgroundColor(mNormalBackgroundColor);
                mNoteBlockHolder.mDividerView.setVisibility(View.VISIBLE);
            }
        }
        ViewCompat.setPaddingRelative(view, paddingStart, paddingTop, paddingEnd, paddingBottom);
        // If status was changed, fade in the view
        if (mStatusChanged) {
            mStatusChanged = false;
            view.setAlpha(0.4f);
            view.animate().alpha(1.0f).start();
        }
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

        mNormalBackgroundColor = ContextExtensionsKt.getColorFromAttribute(
                context,
                com.google.android.material.R.attr.colorSurface
        );
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
