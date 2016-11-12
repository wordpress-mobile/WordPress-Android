package org.wordpress.android.ui.notifications.blocks;

import android.content.Context;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

// Note header, displayed at top of detail view
public class HeaderNoteBlock extends NoteBlock {

    private final JSONArray mHeaderArray;

    private final UserNoteBlock.OnGravatarClickedListener mGravatarClickedListener;
    private Boolean mIsComment;
    private int mAvatarSize;

    private WPNetworkImageView.ImageType mImageType;

    public HeaderNoteBlock(Context context, JSONArray headerArray, WPNetworkImageView.ImageType imageType,
                           OnNoteBlockTextClickListener onNoteBlockTextClickListener,
                           UserNoteBlock.OnGravatarClickedListener onGravatarClickedListener) {
        super(new JSONObject(), onNoteBlockTextClickListener);

        mHeaderArray = headerArray;
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

    @Override
    public View configureView(View view) {
        final NoteHeaderBlockHolder noteBlockHolder = (NoteHeaderBlockHolder)view.getTag();

        Spannable spannable = NotificationsUtils.getSpannableContentForRanges(mHeaderArray.optJSONObject(0));
        noteBlockHolder.nameTextView.setText(spannable);

        noteBlockHolder.avatarImageView.setImageUrl(getAvatarUrl(), mImageType);
        if (!TextUtils.isEmpty(getUserUrl())) {
            noteBlockHolder.avatarImageView.setOnTouchListener(mOnGravatarTouchListener);
        } else {
            noteBlockHolder.avatarImageView.setOnTouchListener(null);
        }

        noteBlockHolder.snippetTextView.setText(getSnippet());

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

    private String getUserName() {
        return JSONUtils.queryJSON(mHeaderArray, "[0].text", "");
    }

    private String getAvatarUrl() {
        return GravatarUtils.fixGravatarUrl(JSONUtils.queryJSON(mHeaderArray, "[0].media[0].url", ""), mAvatarSize);
    }

    private String getUserUrl() {
        return JSONUtils.queryJSON(mHeaderArray, "[0].ranges[0].url", "");
    }

    private String getSnippet() {
        return JSONUtils.queryJSON(mHeaderArray, "[1].text", "");
    }

    @Override
    public Object getViewHolder(View view) {
        return new NoteHeaderBlockHolder(view);
    }

    public void setIsComment(Boolean isComment) {
        mIsComment = isComment;
    }

    private class NoteHeaderBlockHolder {
        private final TextView nameTextView;
        private final TextView snippetTextView;
        private final WPNetworkImageView avatarImageView;

        public NoteHeaderBlockHolder(View view) {
            View rootView = view.findViewById(R.id.header_root_view);
            rootView.setOnClickListener(mOnClickListener);
            nameTextView = (TextView)view.findViewById(R.id.header_user);
            snippetTextView = (TextView)view.findViewById(R.id.header_snippet);
            avatarImageView = (WPNetworkImageView)view.findViewById(R.id.header_avatar);
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
                    long siteId = Long.valueOf(JSONUtils.queryJSON(mHeaderArray, "[0].ranges[0].site_id", 0));
                    long userId = Long.valueOf(JSONUtils.queryJSON(mHeaderArray, "[0].ranges[0].id", 0));
                    String siteUrl = getUserUrl();
                    if (siteId > 0 && userId > 0) {
                        mGravatarClickedListener.onGravatarClicked(siteId, userId, siteUrl);
                    }
                }
            }

            return true;
        }
    };
}
