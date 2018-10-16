package org.wordpress.android.ui.notifications.blocks;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.tools.FormattableRange;
import org.wordpress.android.fluxc.tools.FormattableRangeType;

import java.util.List;

/**
 * A clickable span that includes extra ids/urls
 * Maps to a 'range' in a WordPress.com note object
 */
public class NoteBlockClickableSpan extends ClickableSpan {
    private long mId;
    private long mSiteId;
    private long mPostId;
    private FormattableRangeType mRangeType;
    private FormattableRange mFormattableRange;
    private String mUrl;
    private List<Integer> mIndices;
    private boolean mPressed;
    private boolean mShouldLink;
    private boolean mIsFooter;

    private int mTextColor;
    private int mBackgroundColor;
    private int mLinkColor;
    private int mLightTextColor;


    public NoteBlockClickableSpan(Context context, FormattableRange range, boolean shouldLink, boolean isFooter) {
        mShouldLink = shouldLink;
        mIsFooter = isFooter;

        // Text/background colors
        mTextColor = context.getResources().getColor(R.color.grey_dark);
        mBackgroundColor = context.getResources().getColor(R.color.pressed_wordpress);
        mLinkColor = context.getResources().getColor(R.color.blue_medium);
        mLightTextColor = context.getResources().getColor(R.color.grey_dark);

        processRangeData(range);
    }


    private void processRangeData(FormattableRange range) {
        if (range != null) {
            mFormattableRange = range;
            mId = range.getId() == null ? 0 : range.getId();
            mSiteId = range.getSiteId() == null ? 0 : range.getSiteId();
            mPostId = range.getPostId() == null ? 0 : range.getPostId();
            mRangeType = range.rangeType();
            mUrl = range.getUrl();
            mIndices = range.getIndices();

            mShouldLink = shouldLinkRangeType();

            // Apply grey color to some types
            if (mIsFooter || getRangeType() == FormattableRangeType.BLOCKQUOTE
                || getRangeType() == FormattableRangeType.POST) {
                mTextColor = mLightTextColor;
            }
        }
    }

    // Don't link certain range types, or unknown ones, unless we have a URL
    private boolean shouldLinkRangeType() {
        return mShouldLink
               && mRangeType != FormattableRangeType.BLOCKQUOTE
               && mRangeType != FormattableRangeType.MATCH
               && (mRangeType != FormattableRangeType.UNKNOWN || !TextUtils.isEmpty(mUrl));
    }

    @Override
    public void updateDrawState(@NonNull TextPaint textPaint) {
        // Set background color
        textPaint.bgColor = mShouldLink && mPressed && !isBlockquoteType()
                ? mBackgroundColor : Color.TRANSPARENT;
        textPaint.setColor(mShouldLink && !mIsFooter ? mLinkColor : mTextColor);
        // No underlines
        textPaint.setUnderlineText(mIsFooter);
    }

    private boolean isBlockquoteType() {
        return getRangeType() == FormattableRangeType.BLOCKQUOTE;
    }

    // return the desired style for this id type
    public int getSpanStyle() {
        if (mIsFooter) {
            return Typeface.BOLD;
        }

        switch (getRangeType()) {
            case USER:
            case MATCH:
                return Typeface.BOLD;
            case SITE:
            case POST:
            case COMMENT:
            case BLOCKQUOTE:
                return Typeface.ITALIC;
            case STAT:
            case FOLLOW:
            case NOTICON:
            case LIKE:
            case UNKNOWN:
            default:
                return Typeface.NORMAL;
        }
    }

    @Override
    public void onClick(View widget) {
        // noop
    }

    public FormattableRangeType getRangeType() {
        return mRangeType;
    }

    public FormattableRange getFormattableRange() {
        return mFormattableRange;
    }

    public List<Integer> getIndices() {
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
        mRangeType = FormattableRangeType.Companion.fromString(type);
    }
}
