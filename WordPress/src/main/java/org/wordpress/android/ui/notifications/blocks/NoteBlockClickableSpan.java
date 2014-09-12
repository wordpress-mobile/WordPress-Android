package org.wordpress.android.ui.notifications.blocks;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.ui.notifications.NotificationsConstants;
import org.wordpress.android.util.JSONUtil;

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

    private int mTextColor = NotificationsConstants.COLOR_CALYPSO_DARK_BLUE;

    private final JSONObject mBlockData;

    public NoteBlockClickableSpan(JSONObject idData, boolean shouldLink) {
        mBlockData = idData;
        mShouldLink = shouldLink;
        processRangeData();
    }


    private void processRangeData() {
        if (mBlockData != null) {
            mId = JSONUtil.queryJSON(mBlockData, "id", 0);
            mSiteId = JSONUtil.queryJSON(mBlockData, "site_id", 0);
            mPostId = JSONUtil.queryJSON(mBlockData, "post_id", 0);
            mRangeType = NoteBlockRangeType.fromString(JSONUtil.queryJSON(mBlockData, "type", ""));
            mUrl = JSONUtil.queryJSON(mBlockData, "url", "");
            mIndices = new int[]{0,0};
            JSONArray indicesArray = mBlockData.optJSONArray("indices");
            if (indicesArray != null) {
                mIndices[0] = indicesArray.optInt(0);
                mIndices[1] = indicesArray.optInt(1);
            }

            // Don't link ranges that we don't know the type of, unless we have a URL
            mShouldLink = mShouldLink && (mRangeType != NoteBlockRangeType.UNKNOWN || !TextUtils.isEmpty(mUrl));

            // Apply different coloring for blockquotes
            if (getRangeType() == NoteBlockRangeType.BLOCKQUOTE) {
                mShouldLink = false;
                mTextColor = NotificationsConstants.COLOR_CALYPSO_BLUE;
            }
        }
    }

    @Override
    public void updateDrawState(@Nonnull TextPaint textPaint) {
        // Set background color
        textPaint.bgColor = mPressed && !isBlockquoteType() ? NotificationsConstants.COLOR_CALYPSO_BLUE_BORDER : Color.TRANSPARENT;
        textPaint.setColor(mShouldLink ? NotificationsConstants.COLOR_NEW_KID_BLUE : mTextColor);
        // No underlines
        textPaint.setUnderlineText(false);
    }

    private boolean isBlockquoteType() {
        return getRangeType() == NoteBlockRangeType.BLOCKQUOTE;
    }

    // return the desired style for this id type
    public int getSpanStyle() {
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

    public boolean shouldShowBlogPreview() {
        return mRangeType == NoteBlockRangeType.USER || mRangeType == NoteBlockRangeType.SITE;
    }
}
