package org.wordpress.android.support;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.util.Size;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;

import org.wordpress.android.util.image.ImagePlaceholderManager;
import org.wordpress.android.util.image.ImageType;

import static junit.framework.Assert.fail;


public class PlaceholderComparison {
    private Context mContext;
    private ImagePlaceholderManager mPlaceholderManager = new ImagePlaceholderManager();
    private ImageType mImageType;

    public PlaceholderComparison(ImageType imageType) {
        mContext = InstrumentationRegistry.getTargetContext();
        mImageType = imageType;
    }

    public boolean matches(ImageView view) {
        Drawable drawable = view.getDrawable();
        Size size = new Size(view.getWidth(), view.getHeight());

        return matches(drawable, size);
    }

    private boolean matches(Drawable drawable, Size size) {
        switch (mImageType) {
            case PHOTO:
            case VIDEO:
                return colorComparePlaceholder(drawable);

            case AVATAR: return bitmapComparePlaceholder(drawable, size);
            case BLAVATAR: return bitmapComparePlaceholder(drawable, size);

            default: fail();
        }

        return false;
    }


    private boolean colorComparePlaceholder(Drawable drawable) {
        if (!(drawable instanceof ColorDrawable)) {
            return false;
        }

        int placeholderResource = mPlaceholderManager.getPlaceholderResource(mImageType);
        ColorDrawable template = new ColorDrawable(placeholderResource);
        int templateColor = template.getColor();

        return placeholderResource == templateColor;
    }

    private boolean bitmapComparePlaceholder(Drawable drawable, Size size) {
        if (!(drawable instanceof BitmapDrawable)) {
            return false;
        }

        int placeholderResource = mPlaceholderManager.getPlaceholderResource(mImageType);
        Bitmap placeholder = ((BitmapDrawable) drawable).getBitmap();

        Bitmap template = resourceToBitmap(placeholderResource, size);

        if (mImageType == ImageType.AVATAR) {
            BitmapPool pool = Glide.get(mContext).getBitmapPool();
            template = TransformationUtils.circleCrop(pool, template, size.getWidth(), size.getHeight());
        }

        return placeholder.sameAs(template);
    }

    private Bitmap resourceToBitmap(int resourceId, Size size) {
        Bitmap bitmap = Bitmap.createBitmap(size.getWidth(), size.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Drawable d = mContext.getResources().getDrawable(resourceId);
        d.setBounds(0, 0, size.getWidth(), size.getHeight());
        d.draw(canvas);

        return bitmap;
    }
}
