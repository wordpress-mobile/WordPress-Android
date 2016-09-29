package org.wordpress.android.ui.reader.views;

import android.annotation.TargetApi;
import android.content.Context;
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

/**
 * used by the detail view to display a row of tags from a reader post
 */
public class ReaderTagStrip extends LinearLayout {

    private View mView;
    private LinearLayout mContainer;
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReaderTagStrip(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        mView = inflate(context, R.layout.reader_tag_strip, this);
        mContainer = (LinearLayout) mView.findViewById(R.id.tag_strip_container);
    }

    private void addTag(final String tagName) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.reader_tag_strip_label, mContainer, false);

        TextView txtTag = (TextView) view.findViewById(R.id.text_tag);
        txtTag.setText(mNumTags > 0 ? ", " + tagName : tagName);

        txtTag.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ReaderTag tag = ReaderUtils.createTagFromTagName(tagName, ReaderTagType.FOLLOWED);
                ReaderActivityLauncher.showReaderTagPreview(v.getContext(), tag);
            }
        });

        mContainer.addView(view);
        mNumTags++;
    }

    public void setPost(@NonNull ReaderPost post) {
        mContainer.removeAllViews();
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
}
