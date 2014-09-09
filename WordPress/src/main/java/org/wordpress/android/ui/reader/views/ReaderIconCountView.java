package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.FormatUtils;

/*
 * used when showing comment + comment count, like + like count
 */
public class ReaderIconCountView extends LinearLayout {
    private ImageView mImageView;
    private TextView mTextCount;

    private static final int ICON_LIKE = 0;
    private static final int ICON_COMMENT = 1;

    public ReaderIconCountView(Context context){
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
        mImageView = (ImageView) findViewById(R.id.image_count);
        mTextCount = (TextView) findViewById(R.id.text_count);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.ReaderIconCountView,
                    0, 0);
            try {
                int icon = a.getInteger(R.styleable.ReaderIconCountView_icon, ICON_LIKE);
                switch (icon) {
                    case ICON_LIKE :
                        mImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.reader_button_like));
                        break;
                    case ICON_COMMENT :
                        mImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.reader_button_comment));
                        break;
                }

            } finally {
                a.recycle();
            }
        }
    }

    public ImageView getImageView() {
        return mImageView;
    }

    public void setSelected(boolean selected) {
        mImageView.setSelected(selected);
    }

    public void setCount(int count) {
        if (count > 0) {
            mTextCount.setText(FormatUtils.formatInt(count));
            mTextCount.setVisibility(View.VISIBLE);
        } else {
            mTextCount.setVisibility(View.GONE);
        }
    }
}