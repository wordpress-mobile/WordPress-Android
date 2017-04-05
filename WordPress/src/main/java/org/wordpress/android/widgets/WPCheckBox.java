package org.wordpress.android.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;

import org.wordpress.android.R;

/**
 * Needed to support vector drawables
 *
 * ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5560
 */

public class WPCheckBox extends AppCompatCheckBox {
    public WPCheckBox(Context context) {
        super(context);
    }

    public WPCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        readCustomAttrs(context, attrs);
    }

    public WPCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readCustomAttrs(context, attrs);
    }

    private void readCustomAttrs(Context context, AttributeSet attrs) {
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WPTextView, 0, 0);
        if (array != null) {
            // support vector drawables for API < 5.0
            Drawable drawableLeft = null;
            Drawable drawableRight = null;
            Drawable drawableBottom = null;
            Drawable drawableTop = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                drawableLeft = array.getDrawable(R.styleable.WPTextView_drawableLeftCompat);
                drawableRight = array.getDrawable(R.styleable.WPTextView_drawableRightCompat);
                drawableBottom = array.getDrawable(R.styleable.WPTextView_drawableBottomCompat);
                drawableTop = array.getDrawable(R.styleable.WPTextView_drawableTopCompat);
            } else {
                final int drawableLeftId = array.getResourceId(R.styleable.WPTextView_drawableLeftCompat, -1);
                final int drawableRightId = array.getResourceId(R.styleable.WPTextView_drawableRightCompat, -1);
                final int drawableBottomId = array.getResourceId(R.styleable.WPTextView_drawableBottomCompat, -1);
                final int drawableTopId = array.getResourceId(R.styleable.WPTextView_drawableTopCompat, -1);

                if (drawableLeftId != -1) drawableLeft = AppCompatResources.getDrawable(context, drawableLeftId);
                if (drawableRightId != -1) drawableRight = AppCompatResources.getDrawable(context, drawableRightId);
                if (drawableBottomId != -1) drawableBottom = AppCompatResources.getDrawable(context, drawableBottomId);
                if (drawableTopId != -1) drawableTop = AppCompatResources.getDrawable(context, drawableTopId);
            }
            setCompoundDrawablesWithIntrinsicBounds(drawableLeft, drawableTop, drawableRight, drawableBottom);
            array.recycle();
        }
    }
}
