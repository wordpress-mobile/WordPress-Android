
package org.wordpress.android.util;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader.ImageCache;

public class BitmapLruCache extends LruCache<String, Bitmap> implements ImageCache {

    public BitmapLruCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        int bytes = (value.getRowBytes() * value.getHeight());
        return (bytes / 1024);
    }

    @Override
    public Bitmap getBitmap(String key) {
        return this.get(key);
    }

    @Override
    public void putBitmap(String key, Bitmap bitmap) {
        this.put(key, bitmap);
    }
}
