package org.wordpress.android.ui.reader.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.DisplayUtils;

/**
 * used by the detail view to display the primary and secondary tags from a reader post
 */
public class ReaderTagStrip extends LinearLayout {

    private LinearLayout mView;
    private int mNumTags;
    private String mAllTags;

    public ReaderTagStrip(Context context) {
        super(context);
        initView(context);
    }

    public ReaderTagStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderTagStrip(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReaderTagStrip(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        mView = (LinearLayout) inflate(context, R.layout.reader_tag_strip, this);
    }

    public void setPost(@NonNull ReaderPost post) {
        mView.removeAllViews();
        mNumTags = 0;

        if (post.hasPrimaryTag()) {
            addTag(post.getPrimaryTag());
        }
        if (post.hasSecondaryTag()) {
            addTag(post.getSecondaryTag());
        }

        if (mNumTags == 0) {
            mView.setVisibility(View.GONE);
        } else if (mView.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(mView, AniUtils.Duration.SHORT);
        }
    }

    private void addTag(final String tagName) {
        String tagDisplayName = mNumTags > 0 ? ", " + ReaderUtils.makeHashTag(tagName) : ReaderUtils.makeHashTag(tagName);

        // inflate a new textView to show this tag
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View tagView = inflater.inflate(R.layout.reader_tag_strip_label, mView, false);
        TextView txtTag = (TextView) tagView.findViewById(R.id.text_tag);
        mView.addView(tagView);

        // skip showing this tag if it will make the view wider than the max
        if (mNumTags > 0) {
            // determine the width of the passed tag in the new textView
            Rect rect = new Rect();
            Paint paint = txtTag.getPaint();
            paint.getTextBounds(tagDisplayName, 0, tagDisplayName.length(), rect);
            int tagWidth = rect.width();

            // determine the width of the tag already displayed
            paint.getTextBounds(mAllTags, 0, mAllTags.length(), rect);
            int currentWidth = rect.width();

            int maxWidth = DisplayUtils.getDisplayPixelWidth(getContext());
            if (currentWidth + tagWidth > maxWidth) {
                return;
            }
        }

        txtTag.setText(tagDisplayName);
        mAllTags += tagDisplayName;
        mNumTags++;

        // show all posts with this tag when the tag is clicked
        txtTag.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ReaderTag tag = ReaderUtils.createTagFromTagName(tagName, ReaderTagType.FOLLOWED);
                ReaderActivityLauncher.showReaderTagPreview(v.getContext(), tag);
            }
        });
    }
}
