package org.wordpress.android.ui.notifications.blocks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

// A user block with slightly different formatting for display in a comment detail
public class CommentUserNoteBlock extends UserNoteBlock {
    private CommentStatus mCommentStatus = CommentStatus.APPROVED;
    private int mNormalBackgroundColor;
    private int mNormalTextColor;
    private int mAgoTextColor;
    private int mUnapprovedTextColor;
    private int mIndentedLeftPadding;

    private boolean mStatusChanged;

    public interface OnCommentStatusChangeListener {
        void onCommentStatusChanged(CommentStatus newStatus);
    }

    public CommentUserNoteBlock(Context context, JSONObject noteObject,
                                OnNoteBlockTextClickListener onNoteBlockTextClickListener,
                                OnGravatarClickedListener onGravatarClickedListener) {
        super(context, noteObject, onNoteBlockTextClickListener, onGravatarClickedListener);

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

        noteBlockHolder.mNameTextView.setText(Html.fromHtml("<strong>" + getNoteText().toString() + "</strong>"));
        noteBlockHolder.mAgoTextView.setText(DateTimeUtils.timeSpanFromTimestamp(getTimestamp(),
                                                                                 WordPress.getContext()));
        boolean hasMetaSiteUrl = !TextUtils.isEmpty(getMetaSiteUrl());
        boolean hasMetaHomeTitle = !TextUtils.isEmpty(getMetaHomeTitle());
        if (hasMetaHomeTitle || hasMetaSiteUrl) {
            noteBlockHolder.mBulletTextView.setVisibility(View.VISIBLE);
            noteBlockHolder.mSiteTextView.setVisibility(View.VISIBLE);
            if (hasMetaHomeTitle) {
                noteBlockHolder.mSiteTextView.setText(getMetaHomeTitle());
            } else {
                noteBlockHolder.mSiteTextView.setText(getMetaSiteUrl().replace("http://", "").replace("https://", ""));
            }
        } else {
            noteBlockHolder.mBulletTextView.setVisibility(View.GONE);
            noteBlockHolder.mSiteTextView.setVisibility(View.GONE);
        }

        boolean isPingback = hasMetaSiteUrl && !hasMetaHomeTitle;
//        noteBlockHolder.mBtnReadSource.setVisibility(isPingback ? View.VISIBLE : View.GONE);
//        noteBlockHolder.mBtnSourceDividerView.setVisibility(isPingback ? View.VISIBLE : View.GONE);

        noteBlockHolder.mSiteTextView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        if (hasImageMediaItem()) {
            String imageUrl = GravatarUtils.fixGravatarUrl(getNoteMediaItem().optString("url", ""), getAvatarSize());
            noteBlockHolder.mAvatarImageView.setImageUrl(imageUrl, WPNetworkImageView.ImageType.AVATAR);
            noteBlockHolder.mAvatarImageView.setContentDescription(
                    view.getContext().getString(R.string.profile_picture, getNoteText().toString()));
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
            noteBlockHolder.mAvatarImageView.showDefaultGravatarImageAndNullifyUrl();
            noteBlockHolder.mAvatarImageView.setOnClickListener(null);
            //noinspection AndroidLintClickableViewAccessibility
            noteBlockHolder.mAvatarImageView.setOnTouchListener(null);
            noteBlockHolder.mAvatarImageView.setContentDescription(null);
        }

        noteBlockHolder.mCommentTextView.setText(
                NotificationsUtils.getSpannableContentForRanges(
                        getNoteData().optJSONObject("comment_text"),
                        noteBlockHolder.mCommentTextView,
                        getOnNoteBlockTextClickListener(),
                        false)
                                                );

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

    private long getTimestamp() {
        return getNoteData().optInt("timestamp", 0);
    }

    private boolean hasCommentNestingLevel() {
        try {
            JSONObject commentTextObject = getNoteData().getJSONObject("comment_text");
            return commentTextObject.optInt("nest_level", 0) > 0;
        } catch (JSONException e) {
            return false;
        }
    }

    @Override
    public Object getViewHolder(View view) {
        return new CommentUserNoteBlockHolder(view);
    }

    private class CommentUserNoteBlockHolder {
        private final WPNetworkImageView mAvatarImageView;
        private final TextView mNameTextView;
        private final TextView mAgoTextView;
        private final TextView mBulletTextView;
        private final TextView mSiteTextView;
        private final TextView mCommentTextView;
        private final View mDividerView;
//        private final View mBtnReadSource;
//        private final View mBtnSourceDividerView;

        CommentUserNoteBlockHolder(View view) {
            mNameTextView = (TextView) view.findViewById(R.id.user_name);
            mAgoTextView = (TextView) view.findViewById(R.id.user_comment_ago);
            mAgoTextView.setVisibility(View.VISIBLE);
            mBulletTextView = (TextView) view.findViewById(R.id.user_comment_bullet);
            mSiteTextView = (TextView) view.findViewById(R.id.user_comment_site);
            mCommentTextView = (TextView) view.findViewById(R.id.user_comment);
            mCommentTextView.setMovementMethod(new NoteBlockLinkMovementMethod());
            mAvatarImageView = (WPNetworkImageView) view.findViewById(R.id.user_avatar);
            mDividerView = view.findViewById(R.id.divider_view);
//            mBtnSourceDividerView = view.findViewById(R.id.btn_source_divider_view);
//            mBtnReadSource = view.findViewById(R.id.btn_read_source_post);

            OnClickListener sourceListener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getOnNoteBlockTextClickListener() != null) {
                        getOnNoteBlockTextClickListener().showSitePreview(getMetaSiteId(), getMetaSiteUrl());
                    }
                }
            };
//            mBtnReadSource.setOnClickListener(sourceListener);
            mSiteTextView.setOnClickListener(sourceListener);

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
