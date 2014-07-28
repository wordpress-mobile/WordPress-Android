package org.wordpress.android.ui.notifications.blocks;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.util.JSONUtil;

/**
 * A clickable span that includes extra ids/urls
 * Maps to an 'id' in a WordPress.com note object
 */
public class NoteBlockClickableSpan extends ClickableSpan {
    private long mId;
    private long mSiteId;
    private NoteBlockIdType mType;
    private String mUrl;
    private int[] mIndices;
    private boolean mPressed;
    private int mBackgroundColor;

    private JSONObject mBlockData;

    public NoteBlockClickableSpan(JSONObject idData, int backgroundColor) {
        mBlockData = idData;
        mBackgroundColor = backgroundColor;
        processIdData();
    }


    private void processIdData() {
        if (mBlockData != null) {
            mId = JSONUtil.queryJSON(mBlockData, "id", 0);
            mSiteId = JSONUtil.queryJSON(mBlockData, "site_id", 0);
            mType = NoteBlockIdType.fromString(JSONUtil.queryJSON(mBlockData, "type", ""));
            mUrl = JSONUtil.queryJSON(mBlockData, "url", "");
            mIndices = new int[]{0,0};
            JSONArray indicesArray = mBlockData.optJSONArray("indices");
            if (indicesArray != null) {
                mIndices[0] = indicesArray.optInt(0);
                mIndices[1] = indicesArray.optInt(1);
            }
        }
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        // Set background color
        textPaint.bgColor = mPressed ? mBackgroundColor : Color.TRANSPARENT;
        // No underlines
        textPaint.setUnderlineText(false);
    }

    // return the desired style for this id type
    public int getSpanStyle() {
        switch (getType()) {
            case USER:
                return Typeface.BOLD;
            case SITE:
            case POST:
            case COMMENT:
                return Typeface.ITALIC;
            default:
                return Typeface.NORMAL;
        }
    }

    @Override
    public void onClick(View widget) {
    }

    public NoteBlockIdType getType() {
        return mType;
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

    public void setPressed(boolean isPressed) {
        this.mPressed = isPressed;
    }

    public String getUrl() {
        return mUrl;
    }

    private boolean hasUrl() {
        return !TextUtils.isEmpty(mUrl);
    }

    public boolean shouldShowBlogPreview() {
        return mType == NoteBlockIdType.USER || mType == NoteBlockIdType.SITE;
    }
}
