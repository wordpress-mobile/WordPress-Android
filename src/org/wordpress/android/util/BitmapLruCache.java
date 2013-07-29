
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
    protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
        
        // commenting this out due to "Cannot draw recycled Bitmap errors"
        // see CommonsWare's comment on the accepted answer at:
        // http://stackoverflow.com/questions/12218976/cannot-draw-recycled-bitmaps-when-displaying-bitmaps-in-gallery-attached-to-ad
        
        // if (!oldValue.isRecycled()) {
        // oldValue.recycle();
        // oldValue = null;
        // }
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
