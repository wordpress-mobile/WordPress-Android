package org.wordpress.android.ui.notifications.blocks;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;

import org.json.JSONObject;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.R;

import javax.annotation.Nonnull;

/**
 * A clickable span that includes extra ids/urls
 * Maps to a 'range' in a WordPress.com note object
 */
public class NoteBlockClickableSpan extends ClickableSpan {
    private long mId;
    private long mSiteId;
    private long mPostId;
    private NoteBlockRangeType mRangeType;
    private String mUrl;
    private int[] mIndices;
    private boolean mPressed;
    private boolean mShouldLink;
    private boolean mIsFooter;
    private Context mContext;
    private int mTextColor;

    private final JSONObject mBlockData;

    public NoteBlockClickableSpan(Context context, JSONObject blockData, boolean shouldLink, boolean isFooter) {
        mContext = context;
        mBlockData = blockData;
        mShouldLink = shouldLink;
        mIsFooter = isFooter;
        mTextColor = context.getResources().getColor(R.color.grey_dark);
        processRangeData();
    }


    private void processRangeData() {
        if (mBlockData != null) {
            mId = JSONUtils.queryJSON(mBlockData, "id", 0);
            mSiteId = JSONUtils.queryJSON(mBlockData, "site_id", 0);
            mPostId = JSONUtils.queryJSON(mBlockData, "post_id", 0);
            mRangeType = NoteBlockRangeType.fromString(JSONUtils.queryJSON(mBlockData, "type", ""));
            mUrl = JSONUtils.queryJSON(mBlockData, "url", "");
            mIndices = NotificationsUtils.getIndicesForRange(mBlockData);

            // Don't link certain range types, or unknown ones, unless we have a URL
            mShouldLink = mShouldLink && mRangeType != NoteBlockRangeType.BLOCKQUOTE &&
                    (mRangeType != NoteBlockRangeType.UNKNOWN || !TextUtils.isEmpty(mUrl));

            // Apply grey color to some types
            if (mIsFooter || getRangeType() == NoteBlockRangeType.BLOCKQUOTE || getRangeType() == NoteBlockRangeType.POST) {
                mTextColor = mContext.getResources().getColor(R.color.grey);
            }
        }
    }

    @Override
    public void updateDrawState(@Nonnull TextPaint textPaint) {
        // Set background color
        textPaint.bgColor = mPressed && !isBlockquoteType() ?
                mContext.getResources().getColor(R.color.grey_lighten_20) : Color.TRANSPARENT;
        textPaint.setColor(mShouldLink && !mIsFooter ? mContext.getResources().getColor(R.color.blue_medium) : mTextColor);
        // No underlines
        textPaint.setUnderlineText(mIsFooter);
    }

    private boolean isBlockquoteType() {
        return getRangeType() == NoteBlockRangeType.BLOCKQUOTE;
    }

    // return the desired style for this id type
    public int getSpanStyle() {
        if (mIsFooter) {
            return Typeface.BOLD;
        }

        switch (getRangeType()) {
            case USER:
                return Typeface.BOLD;
            case SITE:
            case POST:
            case COMMENT:
            case BLOCKQUOTE:
                return Typeface.ITALIC;
            default:
                return Typeface.NORMAL;
        }
    }

    @Override
    public void onClick(View widget) {
        // noop
    }

    public NoteBlockRangeType getRangeType() {
        return mRangeType;
    }

    public int[] getIndices() {
        return mIndices;
    }

    public long getId() {
        return mId;
    }

    public long getSiteId() {
        return mSiteId;
    }

    public long getPostId() {
        return mPostId;
    }

    public void setPressed(boolean isPressed) {
        this.mPressed = isPressed;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setCustomType(String type) {
        mRangeType = NoteBlockRangeType.fromString(type);
    }
}
