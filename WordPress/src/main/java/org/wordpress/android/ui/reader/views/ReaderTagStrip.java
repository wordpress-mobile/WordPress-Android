package org.wordpress.android.ui.reader.views;

import android.content.Context;
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

/**
 * used by the detail view to display the primary and secondary tags from a reader post
 */
public class ReaderTagStrip extends LinearLayout {
    private LinearLayout mView;
    private int mNumTags;

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

    public ReaderTagStrip(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        mView = (LinearLayout) inflate(context, R.layout.reader_tag_strip, this);
    }

    public void setPost(@NonNull ReaderPost post) {
        if (mView.getChildCount() > 0) {
            mView.removeAllViews();
        }

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

    private void addTag(@NonNull final String tagName) {
        String tagDisplayName =
                mNumTags > 0 ? ", " + ReaderUtils.makeHashTag(tagName) : ReaderUtils.makeHashTag(tagName);

        // inflate a new textView to show this tag
        LayoutInflater inflater = LayoutInflater.from(getContext());
        TextView txtTag = (TextView) inflater.inflate(R.layout.reader_tag_strip_label, mView, false);
        txtTag.setText(tagDisplayName);
        mView.addView(txtTag);

        mNumTags++;

        // show all posts with this tag when clicked
        txtTag.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ReaderTag tag = ReaderUtils.createTagFromTagName(tagName, ReaderTagType.FOLLOWED);
                ReaderActivityLauncher.showReaderTagPreview(v.getContext(), tag);
            }
        });
    }
}
