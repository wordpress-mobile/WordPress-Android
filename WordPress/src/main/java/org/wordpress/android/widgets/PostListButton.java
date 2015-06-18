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
    private int mButtonType;

    // from attrs.xml
    public static final int BUTTON_EDIT     = 0;
    public static final int BUTTON_VIEW     = 1;
    public static final int BUTTON_PREVIEW  = 2;
    public static final int BUTTON_STATS    = 3;
    public static final int BUTTON_TRASH    = 4;
    public static final int BUTTON_DELETE   = 5;
    public static final int BUTTON_PUBLISH  = 6;
    public static final int BUTTON_MORE     = 7;
    public static final int BUTTON_BACK     = 8;

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

    public void setButtonType(int buttonType) {
        int textResId;
        int iconResId;
        switch (buttonType) {
            case BUTTON_EDIT:
                textResId = R.string.button_edit;
                iconResId = R.drawable.noticon_edit;
                break;
            case BUTTON_VIEW:
                textResId = R.string.button_view;
                iconResId = R.drawable.noticon_view;
                break;
            case BUTTON_PREVIEW:
                textResId = R.string.button_preview;
                iconResId = R.drawable.noticon_view;
                break;
            case BUTTON_STATS:
                textResId = R.string.button_stats;
                iconResId = R.drawable.noticon_stats;
                break;
            case BUTTON_TRASH:
                textResId = R.string.button_trash;
                iconResId = R.drawable.noticon_trash;
                break;
            case BUTTON_DELETE:
                textResId = R.string.button_delete;
                iconResId = R.drawable.noticon_trash;
                break;
            case BUTTON_PUBLISH:
                textResId = R.string.button_publish;
                iconResId = R.drawable.noticon_publish;
                break;
            case BUTTON_MORE:
                textResId = R.string.button_more;
                iconResId = R.drawable.noticon_more;
                break;
            case BUTTON_BACK:
                textResId = R.string.button_back;
                iconResId = R.drawable.noticon_back;
                break;
            default:
                return;
        }

        mButtonType = buttonType;
        mTextView.setText(textResId);
        mImageView.setImageResource(iconResId);
    }

    public int getButtonType() {
        return mButtonType;
    }
}