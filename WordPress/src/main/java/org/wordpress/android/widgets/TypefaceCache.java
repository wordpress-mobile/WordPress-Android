package org.wordpress.android.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import org.wordpress.android.R;

import java.util.Hashtable;

public class TypefaceCache {

    /**
     * Cache used for all views that support custom fonts - defaults to the system font, but
     * Merriweather is also available via the "wpFontFamily" attribute
     */
    public static final int FAMILY_DEFAULT = 0;
    public static final int FAMILY_DEFAULT_LIGHT = 1;
    public static final int FAMILY_MERRIWEATHER = 2;

    private static final Hashtable<String, Typeface> mTypefaceCache = new Hashtable<>();

    public static Typeface getTypeface(Context context) {
        return getTypeface(context, FAMILY_DEFAULT, Typeface.NORMAL);
    }
    public static Typeface getTypeface(Context context, int family, int fontStyle) {
        if (context == null) {
            return null;
        }

        if (family == FAMILY_MERRIWEATHER) {
            final String typefaceName;
            switch (fontStyle) {
                case Typeface.BOLD:
                    typefaceName = "Merriweather-Bold.ttf";
                    break;
                case Typeface.ITALIC:
                    typefaceName = "Merriweather-Italic.ttf";
                    break;
                case Typeface.BOLD_ITALIC:
                    typefaceName = "Merriweather-BoldItalic.ttf";
                    break;
                default:
                    typefaceName = "Merriweather-Regular.ttf";
                    break;
            }
            return getTypefaceForTypefaceName(context, typefaceName);
        }

        // default system font
        if (family == FAMILY_DEFAULT_LIGHT) {
            return Typeface.create("sans-serif-light", fontStyle);
        } else {
            return Typeface.defaultFromStyle(fontStyle);
        }
    }

    /*
     * returns the desired typeface from the cache, loading it from app's assets if necessary
     */
    protected static Typeface getTypefaceForTypefaceName(Context context, String typefaceName) {
        if (!mTypefaceCache.containsKey(typefaceName)) {
            Typeface typeface = Typeface.createFromAsset(context.getApplicationContext().getAssets(), "fonts/"
                    + typefaceName);
            if (typeface != null) {
                mTypefaceCache.put(typefaceName, typeface);
            }
        }

        return mTypefaceCache.get(typefaceName);
    }

    /*
     * sets the typeface for a TextView (or TextView descendant such as EditText or Button) based on
     * the passed attributes, defaults to normal
     */
    protected static void setCustomTypeface(Context context, TextView view, AttributeSet attrs) {
        if (context == null || view == null) return;

        // skip at design-time
        if (view.isInEditMode()) return;

        // default if not set in attributes
        int family = FAMILY_DEFAULT;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WPTextView, 0, 0);
            if (a != null) {
                try {
                    family = a.getInteger(R.styleable.WPTextView_wpFontFamily, FAMILY_DEFAULT);
                } finally {
                    a.recycle();
                }
            }
        }

        // nothing more to do if this is the default system font
        if (family == FAMILY_DEFAULT) {
            return;
        }

        // determine the font style from the existing typeface
        final int fontStyle;
        if (view.getTypeface() != null) {
            boolean isBold = view.getTypeface().isBold();
            boolean isItalic = view.getTypeface().isItalic();
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

        Typeface typeface = getTypeface(context, family, fontStyle);
        if (typeface != null) {
            view.setTypeface(typeface);
        }
    }
}
