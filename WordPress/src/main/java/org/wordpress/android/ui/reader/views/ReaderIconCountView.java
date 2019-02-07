package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;

/*
 * used when showing comment + comment count, like + like count
 */
public class ReaderIconCountView extends LinearLayout {
    private ImageView mImageView;
    private TextView mTextCount;
    private int mIconType;

    // these must match the same values in attrs.xml
    private static final int ICON_LIKE = 0;
    private static final int ICON_COMMENT = 1;

    public ReaderIconCountView(Context context) {
        super(context);
        initView(context, null);
    }

    public ReaderIconCountView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public ReaderIconCountView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        inflate(context, R.layout.reader_icon_count_view, this);

        mImageView = findViewById(R.id.image_count);
        mTextCount = findViewById(R.id.text_count);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.ReaderIconCountView,
                    0, 0);
            try {
                mIconType = a.getInteger(R.styleable.ReaderIconCountView_readerIcon, ICON_LIKE);
                switch (mIconType) {
                    case ICON_LIKE:
                        mImageView.setImageDrawable(ContextCompat.getDrawable(context,
                                R.drawable.reader_button_like));
                        mImageView.setImageTintList(getResources().getColorStateList(
                                R.color.grey_min_orange_jazzy_grey_disabled_selector));
                        break;
                    case ICON_COMMENT:
                        mImageView.setImageDrawable(ContextCompat.getDrawable(context,
                                R.drawable.ic_comment_white_24dp));
                        mImageView.setImageTintList(getResources().getColorStateList(
                                R.color.grey_min_blue_medium_grey_disabled_selector));
                        break;
                }
            } finally {
                a.recycle();
            }
        }

        // move the comment icon down a bit so it aligns with the text baseline
        if (mIconType == ICON_COMMENT) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mImageView.getLayoutParams();
            params.topMargin = context.getResources().getDimensionPixelSize(R.dimen.margin_extra_extra_small);
        }
    }

    public ImageView getImageView() {
        return mImageView;
    }

    public void setSelected(boolean selected) {
        mImageView.setSelected(selected);
        mTextCount.setSelected(selected);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mImageView.setEnabled(enabled);
        mTextCount.setEnabled(enabled);
    }

    public void setCount(int count) {
        mTextCount.setText(count != 0 ? String.valueOf(count) : "");
    }
}
