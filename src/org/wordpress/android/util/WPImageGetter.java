package org.wordpress.android.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.TextUtils;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

/**
 * Created by nbradbury on 10/11/13.
 * ImageGetter for Html.fromHtml()
 * adapted from existing ImageGetter code in NoteCommentFragment
 */
public class WPImageGetter implements Html.ImageGetter {
    private Context mContext;
    private TextView mView;
    private int mMaxSize;

    public WPImageGetter(Context context, TextView view) {
        this(context, view, 0);
    }

    public WPImageGetter(Context context, TextView view, int maxSize) {
        mContext = context.getApplicationContext();
        mView = view;
        mMaxSize = maxSize;
    }

    @Override
    public Drawable getDrawable(String source) {
        if (TextUtils.isEmpty(source))
            return null;

        // images in reader comments may skip "http:" (no idea why) so make sure to add protocol here
        if (source.startsWith("//"))
            source = "http:" + source;

        Drawable loading = mContext.getResources().getDrawable(R.drawable.remote_image);
        Drawable failed = mContext.getResources().getDrawable(R.drawable.remote_failed);
        final RemoteDrawable remote = new RemoteDrawable(loading, failed);

        WordPress.imageLoader.get(source, new ImageLoader.ImageListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                remote.displayFailed();
                mView.invalidate();
            }
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate){
                if (response.getBitmap() != null) {
                    // view is gone? then stop
                    if (mView == null) {
                        return;
                    }
                    Drawable drawable = new BitmapDrawable(mContext.getResources(), response.getBitmap());
                    final int oldHeight = remote.getBounds().height();
                    int maxWidth = mView.getWidth() - mView.getPaddingLeft() - mView.getPaddingRight();
                    if (mMaxSize > 0 && maxWidth > mMaxSize)
                        maxWidth = mMaxSize;
                    remote.setRemoteDrawable(drawable, maxWidth);
                    // TODO: resize image to fit visibly within the TextView
                    // image is from cache? don't need to modify view height
                    if (isImmediate) {
                        return;
                    }
                    int newHeight = remote.getBounds().height();
                    mView.invalidate();
                    // For ICS
                    mView.setHeight(mView.getHeight() + newHeight - oldHeight);
                    // Pre ICS
                    mView.setEllipsize(null);
                }
            }
        });
        return remote;
    }

    private static class RemoteDrawable extends BitmapDrawable {
        protected Drawable mRemoteDrawable;
        protected Drawable mLoadingDrawable;
        protected Drawable mFailedDrawable;
        private boolean mDidFail=false;
        public RemoteDrawable(Drawable loadingDrawable, Drawable failedDrawable){
            mLoadingDrawable = loadingDrawable;
            mFailedDrawable = failedDrawable;
            setBounds(0, 0, mLoadingDrawable.getIntrinsicWidth(), mLoadingDrawable.getIntrinsicHeight());
        }
        public void displayFailed(){
            mDidFail = true;
        }
        public void setBounds(int x, int y, int width, int height){
            super.setBounds(x, y, width, height);
            if (mRemoteDrawable != null) {
                mRemoteDrawable.setBounds(x, y, width, height);
                return;
            }
            if (mLoadingDrawable != null) {
                mLoadingDrawable.setBounds(x, y, width, height);
                mFailedDrawable.setBounds(x, y, width, height);
            }
        }
        public void setRemoteDrawable(Drawable remote){
            mRemoteDrawable = remote;
            setBounds(0, 0, mRemoteDrawable.getIntrinsicWidth(), mRemoteDrawable.getIntrinsicHeight());
        }
        public void setRemoteDrawable(Drawable remote, int maxWidth){
            // null sentinel for now
            if (remote == null) {
                // throw error
                return;
            }
            mRemoteDrawable = remote;
            // determine if we need to scale the image to fit in view
            int imgWidth = remote.getIntrinsicWidth();
            int imgHeight = remote.getIntrinsicHeight();
            float xScale = (float) imgWidth/(float) maxWidth;
            if (xScale > 1.0f) {
                setBounds(0, 0, Math.round(imgWidth/xScale), Math.round(imgHeight/xScale));
            } else {
                setBounds(0, 0, imgWidth, imgHeight);
            }
        }
        public boolean didFail(){
            return mDidFail;
        }
        public void draw(Canvas canvas){
            if (mRemoteDrawable != null) {
                mRemoteDrawable.draw(canvas);
            } else if (didFail()) {
                mFailedDrawable.draw(canvas);
            } else {
                mLoadingDrawable.draw(canvas);
            }
        }
    }
}
