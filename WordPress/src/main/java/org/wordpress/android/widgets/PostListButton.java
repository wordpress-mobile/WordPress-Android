package org.wordpress.android.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.ColorUtils;

/*
 * buttons in footer of post cards
 */
public class PostListButton extends LinearLayout {
    private ImageView mImageView;
    private TextView mTextView;
    private int mButtonType = BUTTON_NONE;

    // from attrs.xml
    public static final int BUTTON_NONE = 0;
    public static final int BUTTON_EDIT = 1;
    public static final int BUTTON_VIEW = 2;
    public static final int BUTTON_PREVIEW = 3;
    public static final int BUTTON_STATS = 4;
    public static final int BUTTON_TRASH = 5;
    public static final int BUTTON_DELETE = 6;
    public static final int BUTTON_PUBLISH = 7;
    public static final int BUTTON_SYNC = 8;
    public static final int BUTTON_MORE = 9;
    public static final int BUTTON_BACK = 10;
    public static final int BUTTON_SUBMIT = 11;
    public static final int BUTTON_RETRY = 12;

    public PostListButton(Context context) {
        super(context);
        initView(context, null);
    }

    public PostListButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public PostListButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        inflate(context, R.layout.post_list_button, this);

        mImageView = findViewById(R.id.image);
        mTextView = findViewById(R.id.text);

        int buttonType = 0;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.wpPostListButton,
                    0, 0);
            try {
                buttonType = a.getInteger(R.styleable.wpPostListButton_wpPostButtonType, 0);
            } finally {
                a.recycle();
            }
        }

        setButtonType(buttonType);
    }


    public int getButtonType() {
        return mButtonType;
    }

    public void setButtonType(int buttonType) {
        if (buttonType == mButtonType) {
            return;
        }

        int color = getTextColorResId(buttonType);
        mButtonType = buttonType;
        ColorUtils.INSTANCE.setImageResourceWithTint(mImageView, getButtonIconResId(buttonType), color);
        mTextView.setText(getButtonTextResId(buttonType));
        mTextView.setTextColor(getContext().getResources().getColor(color));
    }

    public static @StringRes
    int getButtonTextResId(int buttonType) {
        switch (buttonType) {
            case BUTTON_EDIT:
                return R.string.button_edit;
            case BUTTON_VIEW:
                return R.string.button_view;
            case BUTTON_PREVIEW:
                return R.string.button_preview;
            case BUTTON_STATS:
                return R.string.button_stats;
            case BUTTON_TRASH:
                return R.string.button_trash;
            case BUTTON_DELETE:
                return R.string.button_delete;
            case BUTTON_PUBLISH:
                return R.string.button_publish;
            case BUTTON_SYNC:
                return R.string.button_sync;
            case BUTTON_MORE:
                return R.string.button_more;
            case BUTTON_BACK:
                return R.string.button_back;
            case BUTTON_SUBMIT:
                return R.string.submit_for_review;
            case BUTTON_RETRY:
                return R.string.button_retry;
            default:
                return 0;
        }
    }

    public static @DrawableRes
    int getButtonIconResId(int buttonType) {
        switch (buttonType) {
            case BUTTON_EDIT:
                return R.drawable.ic_pencil_white_24dp;
            case BUTTON_VIEW:
            case BUTTON_PREVIEW:
                return R.drawable.ic_external_white_24dp;
            case BUTTON_STATS:
                return R.drawable.ic_stats_alt_white_24dp;
            case BUTTON_TRASH:
            case BUTTON_DELETE:
                return R.drawable.ic_trash_white_24dp;
            case BUTTON_PUBLISH:
            case BUTTON_SYNC:
            case BUTTON_SUBMIT:
                return R.drawable.ic_reader_white_24dp;
            case BUTTON_MORE:
                return R.drawable.ic_ellipsis_white_24dp;
            case BUTTON_BACK:
                return R.drawable.ic_chevron_left_white_24dp;
            case BUTTON_RETRY:
                return R.drawable.ic_refresh_white_24dp;
            default:
                return 0;
        }
    }

    public static @ColorRes
    int getTextColorResId(int buttonType) {
        switch (buttonType) {
            case BUTTON_RETRY:
                return R.color.alert_red;
            default:
                return R.color.blue_wordpress;
        }
    }
}
