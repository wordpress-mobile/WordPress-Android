package org.wordpress.android.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;

import static org.wordpress.android.widgets.PostListButtonType.BUTTON_NONE;

/*
 * buttons in footer of post cards
 */
public class PostListButton extends LinearLayout {
    private ImageView mImageView;
    private TextView mTextView;
    private PostListButtonType mButtonType = BUTTON_NONE;

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


    public PostListButtonType getButtonType() {
        return mButtonType;
    }

    public void setButtonType(int buttonType) {
        if (buttonType == mButtonType.getValue()) {
            return;
        }

        mButtonType = PostListButtonType.fromInt(buttonType);
        loadResourcesForButtonType(mButtonType);
    }

    public void setButtonType(PostListButtonType buttonType) {
        if (buttonType == mButtonType) {
            return;
        }

        mButtonType = buttonType;
        loadResourcesForButtonType(mButtonType);
    }

    private void loadResourcesForButtonType(PostListButtonType buttonType) {
        int color = getContext().getResources().getColor(getTextColorResId(buttonType));
        mImageView.setImageResource(getButtonIconResId(buttonType));
        mImageView.setImageTintList(ColorStateList.valueOf(color));
        mTextView.setText(getButtonTextResId(buttonType));
        mTextView.setTextColor(color);
    }

    private static @StringRes
    int getButtonTextResId(PostListButtonType buttonType) {
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
            case BUTTON_RESTORE:
                return R.string.button_restore;
            case BUTTON_NONE:
            default:
                if (BuildConfig.DEBUG) {
                    throw new IllegalStateException("ButtonType needs to be assigned.");
                }
                return 0;
        }
    }

    private static @DrawableRes
    int getButtonIconResId(PostListButtonType buttonType) {
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
            case BUTTON_RESTORE:
                // TODO add restore icon
                return R.drawable.ic_pencil_white_24dp;
            case BUTTON_NONE:
                // fall through
            default:
                if (BuildConfig.DEBUG) {
                    throw new IllegalStateException("ButtonType needs to be assigned.");
                }
                return 0;
        }
    }

    private static @ColorRes
    int getTextColorResId(PostListButtonType buttonType) {
        switch (buttonType) {
            case BUTTON_RETRY:
                return R.color.alert_red;
            case BUTTON_NONE:
                // fall through
            case BUTTON_EDIT:
                // fall through
            case BUTTON_VIEW:
                // fall through
            case BUTTON_PREVIEW:
                // fall through
            case BUTTON_STATS:
                // fall through
            case BUTTON_TRASH:
                // fall through
            case BUTTON_DELETE:
                // fall through
            case BUTTON_PUBLISH:
                // fall through
            case BUTTON_SYNC:
                // fall through
            case BUTTON_MORE:
                // fall through
            case BUTTON_BACK:
                // fall through
            case BUTTON_SUBMIT:
                // fall through
            case BUTTON_RESTORE:
                // fall through
            default:
                return R.color.wp_grey_darken_20;
        }
    }
}
