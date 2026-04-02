package org.wordpress.android.widgets;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Hashtable;

public class TypefaceCache {
    /**
     * Cache used for all views that support custom fonts - only used for noticons for now.
     */
    private static final Hashtable<String, Typeface> TYPEFACE_CACHE = new Hashtable<>();

    /**
     * returns the desired typeface from the cache, loading it from app's assets if necessary
     */
    protected static Typeface getTypefaceForTypefaceName(Context context, String typefaceName) {
        if (!TYPEFACE_CACHE.containsKey(typefaceName)) {
            Typeface typeface = Typeface.createFromAsset(context.getApplicationContext().getAssets(),
                                                         "fonts/" + typefaceName);
            if (typeface != null) {
                TYPEFACE_CACHE.put(typefaceName, typeface);
            }
        }

        return TYPEFACE_CACHE.get(typefaceName);
    }
}
