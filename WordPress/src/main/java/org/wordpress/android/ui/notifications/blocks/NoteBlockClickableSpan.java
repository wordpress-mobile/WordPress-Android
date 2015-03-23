package org.wordpress.android.ui.notifications.blocks;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.R;
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
    private Context mContext;
    private int mTextColor;

    private final JSONObject mBlockData;

    public NoteBlockClickableSpan(Context context, JSONObject idData, boolean shouldLink) {
        mBlockData = idData;
        mShouldLink = shouldLink;
        mContext = context;
        mTextColor = context.getResources().getColor(R.color.grey_dark);
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

            // Don't link certain range types, or unknown ones, unless we have a URL
            mShouldLink = mShouldLink && mRangeType != NoteBlockRangeType.BLOCKQUOTE &&
                    (mRangeType != NoteBlockRangeType.UNKNOWN || !TextUtils.isEmpty(mUrl));

            // Apply grey color to some types
            if (getRangeType() == NoteBlockRangeType.BLOCKQUOTE || getRangeType() == NoteBlockRangeType.POST) {
                mTextColor = mContext.getResources().getColor(R.color.grey);
            }
        }
    }

    @Override
    public void updateDrawState(@Nonnull TextPaint textPaint) {
        // Set background color
        textPaint.bgColor = mPressed && !isBlockquoteType() ?
                mContext.getResources().getColor(R.color.grey_lighten_20) : Color.TRANSPARENT;
        textPaint.setColor(mShouldLink ? mContext.getResources().getColor(R.color.blue_medium) : mTextColor);
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
}
