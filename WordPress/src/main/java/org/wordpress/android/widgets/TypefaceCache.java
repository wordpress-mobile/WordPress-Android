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
     * Cache used for all views that support custom fonts. The default is Open Sans, but
     * Merriweather is also available via the "fontFamily" attribute
     */

    private static final int VARIATION_NORMAL = 0;
    private static final int VARIATION_LIGHT = 1;
    private static final int VARIATION_DEFAULT = VARIATION_NORMAL;

    private static final int FAMILY_OPEN_SANS = 0;
    private static final int FAMILY_MERRIWEATHER = 1;
    private static final int FAMILY_DEFAULT = FAMILY_OPEN_SANS;

    private static final Hashtable<String, Typeface> mTypefaceCache = new Hashtable<>();

    public static Typeface getTypeface(Context context) {
        return getTypeface(context, FAMILY_DEFAULT, Typeface.NORMAL, VARIATION_DEFAULT);
    }
    private static Typeface getTypeface(Context context,
                                        int family,
                                        int fontStyle,
                                        int variation) {
        if (context == null) {
            return null;
        }

        final String typefaceName;
        if (family == FAMILY_MERRIWEATHER) {
            if (variation == VARIATION_LIGHT) {
                // note that there isn't a light bold style
                switch (fontStyle) {
                    case Typeface.ITALIC:
                    case Typeface.BOLD_ITALIC:
                        typefaceName = "Merriweather-LightItalic.otf";
                        break;
                    default:
                        typefaceName = "Merriweather-Light.ttf";
                        break;
                }
            } else {
                switch (fontStyle) {
                    case Typeface.BOLD:
                        typefaceName = "Merriweather-Bold.ttf";
                        break;
                    case Typeface.ITALIC:
                        typefaceName = "Merriweather-Italic.otf";
                        break;
                    case Typeface.BOLD_ITALIC:
                        typefaceName = "Merriweather-BoldItalic.otf";
                        break;
                    default:
                        typefaceName = "Merriweather-Regular.ttf";
                        break;
                }
            }
        } else {
            // Open Sans
            if (variation == VARIATION_LIGHT) {
                switch (fontStyle) {
                    case Typeface.BOLD:
                        typefaceName = "OpenSans-LightBold.ttf";
                        break;
                    case Typeface.ITALIC:
                        typefaceName = "OpenSans-LightItalic.ttf";
                        break;
                    case Typeface.BOLD_ITALIC:
                        typefaceName = "OpenSans-LightBoldItalic.ttf";
                        break;
                    default:
                        typefaceName = "OpenSans-Light.ttf";
                        break;
                }
            } else {
                switch (fontStyle) {
                    case Typeface.BOLD:
                        typefaceName = "OpenSans-Bold.ttf";
                        break;
                    case Typeface.ITALIC:
                        typefaceName = "OpenSans-Italic.ttf";
                        break;
                    case Typeface.BOLD_ITALIC:
                        typefaceName = "OpenSans-BoldItalic.ttf";
                        break;
                    default:
                        typefaceName = "OpenSans-Regular.ttf";
                        break;
                }
            }
        }

        return getTypefaceForTypefaceName(context, typefaceName);
    }

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
     * the passed attributes, defaults to normal Open Sans
     */
    protected static void setCustomTypeface(Context context, TextView view, AttributeSet attrs) {
        if (context == null || view == null) return;

        // skip at design-time
        if (view.isInEditMode()) return;

        // defaults if not set in attributes
        int family = FAMILY_DEFAULT;
        int variation = VARIATION_DEFAULT;

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WPTextView, 0, 0);
            if (a != null) {
                try {
                    family = a.getInteger(R.styleable.WPTextView_fontFamily, FAMILY_DEFAULT);
                    variation = a.getInteger(R.styleable.WPTextView_fontVariation, VARIATION_DEFAULT);
                } finally {
                    a.recycle();
                }
            }
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

        Typeface typeface = getTypeface(context, family, fontStyle, variation);
        if (typeface != null) {
            view.setTypeface(typeface);
        }
    }
}
