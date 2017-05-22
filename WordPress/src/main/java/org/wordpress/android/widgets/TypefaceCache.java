package org.wordpress.android.widgets;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Hashtable;

public class TypefaceCache {
    /**
     * Cache used for all views that support custom fonts - only used for noticons for now.
     */
    private static final Hashtable<String, Typeface> mTypefaceCache = new Hashtable<>();

    /**
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
}
