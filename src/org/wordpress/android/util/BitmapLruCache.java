
package org.wordpress.android.util;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader.ImageCache;

public class BitmapLruCache extends LruCache<String, Bitmap> implements ImageCache {

    public BitmapLruCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        // The cache size will be measured in kilobytes rather than
        // number of items.
        
        int bytes = 0;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            bytes = (value.getRowBytes() * value.getHeight());
        } else {
            bytes = value.getByteCount();
        }
        
        if (bytes!=0)
            bytes = bytes / 1024;
        
        return bytes;
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
