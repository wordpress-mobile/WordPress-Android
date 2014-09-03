//Add WordPress image fields to ImageSpan object

package org.wordpress.android.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.text.style.ImageSpan;

import org.wordpress.android.R;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.ui.posts.EditPostActivity;

public class WPImageSpan extends ImageSpan {
    private Context mContext;

    private Uri mImageSource = null;
    private boolean mNetworkImageLoaded = false;
    private boolean mIsInPostEditor;

    private MediaFile mMediaFile;

    public WPImageSpan(Context context, Bitmap b, Uri src) {
        super(context, b);
        this.mImageSource = src;
        mContext = context;
        mMediaFile = new MediaFile();
        if (mContext instanceof EditPostActivity) {
            EditPostActivity editPostActivity = (EditPostActivity)mContext;
            if (editPostActivity.isEditingPostContent())
                mIsInPostEditor = true;
        }

    }

    public WPImageSpan(Context context, int resId, Uri src) {
        super(context, resId);
        this.mImageSource = src;
        mContext = context;
        mMediaFile = new MediaFile();
        if (mContext instanceof EditPostActivity)
            mIsInPostEditor = true;
    }

    public MediaFile getMediaFile() {
        return mMediaFile;
    }

    public void setMediaFile(MediaFile mMediaFile) {
        this.mMediaFile = mMediaFile;
    }

    public void setImageSource(Uri mImageSource) {
        this.mImageSource = mImageSource;
    }

    public Uri getImageSource() {
        return mImageSource;
    }

    public boolean isNetworkImageLoaded() {
        return mNetworkImageLoaded;
    }

    public void setNetworkImageLoaded(boolean networkImageLoaded) {
        this.mNetworkImageLoaded = networkImageLoaded;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, Paint paint) {
        super.draw(canvas, text, start, end, x, top, y, bottom, paint);

        if (mIsInPostEditor && !mMediaFile.isVideo()) {
            // Add 'edit' icon at bottom right of image
            int width = getSize(paint, text, start, end, paint.getFontMetricsInt());
            Bitmap editIconBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ab_icon_edit);
            float editIconXPosition = (x + width) - editIconBitmap.getWidth();
            float editIconYPosition = bottom - editIconBitmap.getHeight();
            // Add a black background with a bit of alpha
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.argb(200, 0, 0, 0));
            canvas.drawRect(editIconXPosition, editIconYPosition, editIconXPosition + editIconBitmap.getWidth(), editIconYPosition + editIconBitmap.getHeight(), bgPaint);
            // Add the icon to the canvas
            canvas.drawBitmap(editIconBitmap, editIconXPosition, editIconYPosition, paint);
        }
    }
}
