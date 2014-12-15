package org.wordpress.android.widgets;

/**
 * A Drawable object used to display text content.
 *
 * Based on https://github.com/devunwired/textdrawable
 */

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;

/**
 * A Drawable object that draws text.
 * A TextDrawable accepts most of the same parameters that can be applied to
 * {@link android.widget.TextView} for displaying and formatting text.
 *
 * Optionally, a {@link Path} may be supplied on which to draw the text.
 *
 * A TextDrawable has an intrinsic size equal to that required to draw all
 * the text it has been supplied, when possible.  In cases where a {@link Path}
 * has been supplied, the caller must explicitly call
 * {@link #setBounds(android.graphics.Rect) setBounds()} to provide the Drawable
 * size based on the Path constraints.
 */
public class TextDrawable extends Drawable {

    /* Platform XML constants for typeface */
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int MONOSPACE = 3;

    /* Resources for scaling values to the given device */
    private Resources mResources;
    /* Paint to hold most drawing primitives for the text */
    private TextPaint mTextPaint;
    /* Layout is used to measure and draw the text */
    private StaticLayout mTextLayout;
    /* Alignment of the text inside its bounds */
    private Layout.Alignment mTextAlignment = Layout.Alignment.ALIGN_NORMAL;
    /* Optional path on which to draw the text */
    private Path mTextPath;
    /* Stateful text color list */
    private ColorStateList mTextColors;
    /* Container for the bounds to be reported to widgets */
    private Rect mTextBounds;
    /* Text string to draw */
    private CharSequence mText = "";

    /* Attribute lists to pull default values from the current theme */
    private static final int[] themeAttributes = {
            android.R.attr.textAppearance
    };
    private static final int[] appearanceAttributes = {
            android.R.attr.textSize,
            android.R.attr.typeface,
            android.R.attr.textStyle,
            android.R.attr.textColor
    };


    public TextDrawable(Context context) {
        super();
        //Used to load and scale resource items
        mResources = context.getResources();
        //Definition of this drawables size
        mTextBounds = new Rect();
        //Paint to use for the text
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.density = mResources.getDisplayMetrics().density;
        mTextPaint.setDither(true);

        int textSize = 15;
        ColorStateList textColor = null;
        int styleIndex = -1;
        int typefaceIndex = -1;

        //Set default parameters from the current theme
        TypedArray a = context.getTheme().obtainStyledAttributes(themeAttributes);
        int appearanceId = a.getResourceId(0, -1);
        a.recycle();

        TypedArray ap = null;
        if (appearanceId != -1) {
            ap = context.obtainStyledAttributes(appearanceId, appearanceAttributes);
        }
        if (ap != null) {
            for (int i=0; i < ap.getIndexCount(); i++) {
                int attr = ap.getIndex(i);
                switch (attr) {
                    case 0: //Text Size
                        textSize = a.getDimensionPixelSize(attr, textSize);
                        break;
                    case 1: //Typeface
                        typefaceIndex = a.getInt(attr, typefaceIndex);
                        break;
                    case 2: //Text Style
                        styleIndex = a.getInt(attr, styleIndex);
                        break;
                    case 3: //Text Color
                        textColor = a.getColorStateList(attr);
                        break;
                    default:
                        break;
                }
            }

            ap.recycle();
        }

        setTextColor(textColor != null ? textColor : ColorStateList.valueOf(0xFF000000));
        setRawTextSize(textSize);

        Typeface tf = null;
        switch (typefaceIndex) {
            case SANS:
                tf = Typeface.SANS_SERIF;
                break;

            case SERIF:
                tf = Typeface.SERIF;
                break;

            case MONOSPACE:
                tf = Typeface.MONOSPACE;
                break;
        }

        setTypeface(tf, styleIndex);
    }


    public void setText(int text) {
       this.setText(String.valueOf(text));
    }

    /**
     * Set the text that will be displayed
     * @param text Text to display
     */
    public void setText(CharSequence text) {
        if (text == null) text = "";

        mText = text;

        measureContent();
    }

    /**
     * Return the text currently being displayed
     */
    public CharSequence getText() {
        return mText;
    }

    /**
     * Return the current text size, in pixels
     */
    public float getTextSize() {
        return mTextPaint.getTextSize();
    }

    /**
     * Set the text size.  The value will be interpreted in "sp" units
     * @param size Text size value, in sp
     */
    public void setTextSize(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    /**
     * Set the text size, using the supplied complex units
     * @param unit Units for the text size, such as dp or sp
     * @param size Text size value
     */
    public void setTextSize(int unit, float size) {
        float dimension = TypedValue.applyDimension(unit, size,
                mResources.getDisplayMetrics());
        setRawTextSize(dimension);
    }

    /*
     * Set the text size, in raw pixels
     */
    private void setRawTextSize(float size) {
        if (size != mTextPaint.getTextSize()) {
            mTextPaint.setTextSize(size);

            measureContent();
        }
    }

    /**
     * Return the horizontal stretch factor of the text
     */
    public float getTextScaleX() {
        return mTextPaint.getTextScaleX();
    }

    /**
     * Set the horizontal stretch factor of the text
     * @param size Text scale factor
     */
    public void setTextScaleX(float size) {
        if (size != mTextPaint.getTextScaleX()) {
            mTextPaint.setTextScaleX(size);
            measureContent();
        }
    }

    /**
     * Return the current text alignment setting
     */
    public Layout.Alignment getTextAlign() {
        return mTextAlignment;
    }

    /**
     * Set the text alignment.  The alignment itself is based on the text layout direction.
     * For LTR text NORMAL is left aligned and OPPOSITE is right aligned.
     * For RTL text, those alignments are reversed.
     * @param align Text alignment value.  Should be set to one of:
     *
     *   {@link Layout.Alignment#ALIGN_NORMAL},
     *   {@link Layout.Alignment#ALIGN_NORMAL},
     *   {@link Layout.Alignment#ALIGN_OPPOSITE}.
     */
    public void setTextAlign(Layout.Alignment align) {
        if (mTextAlignment != align) {
            mTextAlignment = align;
            measureContent();
        }
    }

    /**
     * Sets the typeface and style in which the text should be displayed.
     * Note that not all Typeface families actually have bold and italic
     * variants, so you may need to use
     * {@link #setTypeface(Typeface, int)} to get the appearance
     * that you actually want.
     */
    public void setTypeface(Typeface tf) {
        if (mTextPaint.getTypeface() != tf) {
            mTextPaint.setTypeface(tf);

            measureContent();
        }
    }

    /**
     * Sets the typeface and style in which the text should be displayed,
     * and turns on the fake bold and italic bits in the Paint if the
     * Typeface that you provided does not have all the bits in the
     * style that you specified.
     *
     */
    public void setTypeface(Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }

            setTypeface(tf);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSkewX(0);
            setTypeface(tf);
        }
    }

    /**
     * Return the current typeface and style that the Paint
     * using for display.
     */
    public Typeface getTypeface() {
        return mTextPaint.getTypeface();
    }

    /**
     * Set a single text color for all states
     * @param color Color value such as {@link Color#WHITE} or {@link Color#argb(int, int, int, int)}
     */
    public void setTextColor(int color) {
        setTextColor(ColorStateList.valueOf(color));
    }

    /**
     * Set the text color as a state list
     * @param colorStateList ColorStateList of text colors, such as inflated from an R.color resource
     */
    public void setTextColor(ColorStateList colorStateList) {
        mTextColors = colorStateList;
        updateTextColors(getState());
    }

    /**
     * Optional Path object on which to draw the text.  If this is set,
     * TextDrawable cannot properly measure the bounds this drawable will need.
     * You must call {@link #setBounds(int, int, int, int) setBounds()} before
     * applying this TextDrawable to any View.
     *
     * Calling this method with <code>null</code> will remove any Path currently attached.
     */
    public void setTextPath(Path path) {
        if (mTextPath != path) {
            mTextPath = path;
            measureContent();
        }
    }

    /**
     * Internal method to take measurements of the current contents and apply
     * the correct bounds when possible.
     */
    private void measureContent() {
        //If drawing to a path, we cannot measure intrinsic bounds
        //We must resly on setBounds being called externally
        if (mTextPath != null) {
            //Clear any previous measurement
            mTextLayout = null;
            mTextBounds.setEmpty();
        } else {
            //Measure text bounds
            double desired = Math.ceil( Layout.getDesiredWidth(mText, mTextPaint) );
            mTextLayout = new StaticLayout(mText, mTextPaint, (int)desired,
                    mTextAlignment, 1.0f, 0.0f, false);
            mTextBounds.set(0, 0, mTextLayout.getWidth(), mTextLayout.getHeight());
        }

        //We may need to be redrawn
        invalidateSelf();
    }

    /**
     * Internal method to apply the correct text color based on the drawable's state
     */
    private boolean updateTextColors(int[] stateSet) {
        int newColor = mTextColors.getColorForState(stateSet, Color.WHITE);
        if (mTextPaint.getColor() != newColor) {
            mTextPaint.setColor(newColor);
            return  true;
        }

        return false;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        //Update the internal bounds in response to any external requests
        mTextBounds.set(bounds);
    }

    @Override
    public boolean isStateful() {
        /*
         * The drawable's ability to represent state is based on
         * the text color list set
         */
        return mTextColors.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        //Upon state changes, grab the correct text color
        return updateTextColors(state);
    }

    @Override
    public int getIntrinsicHeight() {
        //Return the vertical bounds measured, or -1 if none
        if (mTextBounds.isEmpty()) {
            return -1;
        } else {
            return (mTextBounds.bottom - mTextBounds.top);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        //Return the horizontal bounds measured, or -1 if none
        if (mTextBounds.isEmpty()) {
            return -1;
        } else {
            return (mTextBounds.right - mTextBounds.left);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        final Rect bounds = getBounds();
        final int count = canvas.save();
        canvas.translate(bounds.left, bounds.top);
        if (mTextPath == null) {
            //Allow the layout to draw the text
            mTextLayout.draw(canvas);
        } else {
            //Draw directly on the canvas using the supplied path
            canvas.drawTextOnPath(mText.toString(), mTextPath, 0, 0, mTextPaint);
        }
        canvas.restoreToCount(count);
    }

    @Override
    public void setAlpha(int alpha) {
        if (mTextPaint.getAlpha() != alpha) {
            mTextPaint.setAlpha(alpha);
        }
    }

    @Override
    public int getOpacity() {
        return mTextPaint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (mTextPaint.getColorFilter() != cf) {
            mTextPaint.setColorFilter(cf);
        }
    }

}