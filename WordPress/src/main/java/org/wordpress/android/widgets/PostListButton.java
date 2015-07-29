package org.wordpress.android.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;

/*
 * buttons in footer of post cards
 */
public class PostListButton extends LinearLayout {

    private ImageView mImageView;
    private TextView mTextView;
    private int mButtonType = BUTTON_NONE;

    // from attrs.xml
    public static final int BUTTON_NONE     = 0;
    public static final int BUTTON_EDIT     = 1;
    public static final int BUTTON_VIEW     = 2;
    public static final int BUTTON_PREVIEW  = 3;
    public static final int BUTTON_STATS    = 4;
    public static final int BUTTON_TRASH    = 5;
    public static final int BUTTON_DELETE   = 6;
    public static final int BUTTON_PUBLISH  = 7;
    public static final int BUTTON_MORE     = 8;
    public static final int BUTTON_BACK     = 9;

    public PostListButton(Context context){
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

        mImageView = (ImageView) findViewById(R.id.image);
        mTextView = (TextView) findViewById(R.id.text);

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

        mButtonType = buttonType;
        mTextView.setText(getButtonTextResId(buttonType));
        mImageView.setImageResource(getButtonIconResId(buttonType));
    }

    public static int getButtonTextResId(int buttonType) {
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
            case BUTTON_MORE:
                return R.string.button_more;
            case BUTTON_BACK:
                return R.string.button_back;
            default:
                return 0;
        }
    }

    public static int getButtonIconResId(int buttonType) {
        switch (buttonType) {
            case BUTTON_EDIT:
                return R.drawable.noticon_edit;
            case BUTTON_VIEW:
                return R.drawable.noticon_view;
            case BUTTON_PREVIEW:
                return R.drawable.noticon_view;
            case BUTTON_STATS:
                return R.drawable.noticon_stats;
            case BUTTON_TRASH:
                return R.drawable.noticon_trash;
            case BUTTON_DELETE:
                return R.drawable.noticon_trash;
            case BUTTON_PUBLISH:
                return R.drawable.noticon_publish;
            case BUTTON_MORE:
                return R.drawable.noticon_more;
            case BUTTON_BACK:
                return R.drawable.noticon_back;
            default:
                return 0;
        }
    }
}