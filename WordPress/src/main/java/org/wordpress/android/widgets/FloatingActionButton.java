package org.wordpress.android.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageButton;

import org.wordpress.android.R;
import org.wordpress.android.util.DisplayUtils;

public class FloatingActionButton extends ImageButton {
    private boolean mIsLollipop;

    public FloatingActionButton(Context context) {
        super(context);
        init(context);
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    @SuppressLint("NewApi")
    void init(Context context) {
        mIsLollipop = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
        if (mIsLollipop) {
            setElevation(DisplayUtils.dpToPx(context, 4));
        }

        int fabColor = context.getResources().getColor(R.color.fab_color);
        int fabColorPressed = darken(fabColor);

        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_pressed}, createOval(fabColorPressed));
        background.addState(new int[]{}, createOval(fabColor));
        setBackgroundCompat(background);
    }

    private int darken(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }


    private Drawable createOval(int color) {
        ShapeDrawable shape = new ShapeDrawable(new OvalShape());
        shape.getPaint().setColor(color);
        shape.getPaint().setStyle(Paint.Style.FILL);

        // on lollipop we let elevation take care of the shadow
        if (mIsLollipop) {
            return shape;
        }

        Drawable shadow = getContext().getResources().getDrawable(R.drawable.fab_shadow);
        Drawable[] layers = {shadow, shape};
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        // inset the shape drawable so the shadow is uncovered
        int oneDp = DisplayUtils.dpToPx(getContext(), 1);
        int twoDp = DisplayUtils.dpToPx(getContext(), 2);
        layerDrawable.setLayerInset(1, oneDp, oneDp, twoDp, twoDp);

        return layerDrawable;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void setBackgroundCompat(Drawable drawable) {
        if (mIsLollipop) {
            int rippleColor = getContext().getResources().getColor(R.color.fab_color);
            RippleDrawable rippleDrawable = new RippleDrawable(
                    new ColorStateList(new int[][]{{}}, new int[]{rippleColor}), drawable, null);
            setBackground(rippleDrawable);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(drawable);
        } else {
            setBackgroundDrawable(drawable);
        }
    }
}