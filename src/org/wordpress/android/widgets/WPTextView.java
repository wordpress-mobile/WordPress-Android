package org.wordpress.android.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import org.wordpress.android.R;

import java.util.Hashtable;

/**
 * Created by nbradbury on 7/15/13.
 * custom TextView used in layouts - enables keeping custom typeface handling in one place (so we
 * avoid having to set the typeface for every single TextView in every single activity)
 */
public class WPTextView extends TextView {
    private static final int VARIATION_NORMAL = 0;
    private static final int VARIATION_LIGHT = 1;

    public WPTextView(Context context) {
        super(context);
        setCustomFont(context, null);
    }
    public WPTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCustomFont(context, attrs);
    }
    public WPTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setCustomFont(context, attrs);
    }

    private void setCustomFont(Context context, AttributeSet attrs) {
        // skip at design-time
        if (this.isInEditMode())
            return;

        // read custom fontVariation from attributes, default to normal
        int variation = VARIATION_NORMAL;
        if (attrs!=null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.WPTextView,
                    0, 0);

            if (a!=null) {
                try {
                    variation = a.getInteger(R.styleable.WPTextView_fontVariation, VARIATION_NORMAL);
                } finally {
                    a.recycle();
                }
            }
        }

        // determine the font style from the existing typeface
        final int fontStyle;
        if (this.getTypeface()!=null) {
            boolean isBold = this.getTypeface().isBold();
            boolean isItalic = this.getTypeface().isItalic();
            if (isBold && isItalic) {
                fontStyle = Typeface.BOLD_ITALIC;
            } else if (isBold) {
                fontStyle = Typeface.BOLD;
            } else if (isItalic) {
                fontStyle = Typeface.ITALIC;
            } else {
                fontStyle = Typeface.NORMAL;
            }
        } else {
            fontStyle = Typeface.NORMAL;
        }

        Typeface typeface = getCustomTypeface(context, fontStyle, variation);
        if (typeface!=null)
            setTypeface(typeface);
    }

    private static final Hashtable<String, Typeface> mTypefaceCache = new Hashtable<String, Typeface>();

    private static Typeface getCustomTypeface(Context context, int fontStyle, int variation) {
        // note that the "light" variation doesn't support bold or bold-italic
        final String typefaceName;
        switch (fontStyle) {
            case Typeface.BOLD :
                typefaceName = "OpenSans-Bold.ttf";
                break;
            case Typeface.ITALIC :
                typefaceName = (variation==VARIATION_LIGHT ? "OpenSans-LightItalic.ttf" : "OpenSans-Italic.ttf");
                break;
            case Typeface.BOLD_ITALIC :
                typefaceName = "OpenSans-BoldItalic.ttf";
                break;
            default :
                typefaceName = (variation==VARIATION_LIGHT ? "OpenSans-Light.ttf" : "OpenSans-Regular.ttf");
                break;
        }


        if (!mTypefaceCache.containsKey(typefaceName)) {
            Typeface typeface = Typeface.createFromAsset(context.getApplicationContext().getAssets(), "fonts/" + typefaceName);
            if (typeface!=null)
                mTypefaceCache.put(typefaceName, typeface);
        }

        return mTypefaceCache.get(typefaceName);
    }
}
