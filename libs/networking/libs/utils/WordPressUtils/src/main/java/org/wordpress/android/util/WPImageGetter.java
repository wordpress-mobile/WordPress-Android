package org.wordpress.android.util;

import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.TextUtils;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.util.AppLog.T;

import java.lang.ref.WeakReference;

/**
 * ImageGetter for Html.fromHtml()
 * adapted from existing ImageGetter code in NoteCommentFragment
 */
public class WPImageGetter implements Html.ImageGetter {
    private WeakReference<TextView> mWeakView;
    private int mMaxSize;
    private ImageLoader mImageLoader;
    private Drawable mLoadingDrawable;
    private Drawable mFailedDrawable;

    public WPImageGetter(TextView view) {
        this(view, 0);
    }

    public WPImageGetter(TextView view, int maxSize) {
        mWeakView = new WeakReference<TextView>(view);
        mMaxSize = maxSize;
    }

    public WPImageGetter(TextView view, int maxSize, ImageLoader imageLoader, Drawable loadingDrawable,
                         Drawable failedDrawable) {
        mWeakView = new WeakReference<TextView>(view);
        mMaxSize = maxSize;
        mImageLoader = imageLoader;
        mLoadingDrawable = loadingDrawable;
        mFailedDrawable = failedDrawable;
    }

    public void setImageLoader(ImageLoader imageLoader) {
        mImageLoader = imageLoader;
    }

    public void setLoadingDrawable(Drawable loadingDrawable) {
        mLoadingDrawable = loadingDrawable;
    }

    public void setFailedDrawable(Drawable failedDrawable) {
        mFailedDrawable = failedDrawable;
    }

    private TextView getView() {
        return mWeakView.get();
    }

    @Override
    public Drawable getDrawable(String source) {
        if (mImageLoader == null || mLoadingDrawable == null || mFailedDrawable == null) {
            throw new RuntimeException("Developer, you need to call setImageLoader, setLoadingDrawable and setFailedDrawable");
        }

        if (TextUtils.isEmpty(source)) {
            return null;
        }

        // images in reader comments may skip "http:" (no idea why) so make sure to add protocol here
        if (source.startsWith("//")) {
            source = "http:" + source;
        }

        // use Photon if a max size is requested (otherwise the full-sized image will be downloaded
        // and then resized)
        if (mMaxSize > 0) {
            source = PhotonUtils.getPhotonImageUrl(source, mMaxSize, 0);
        }

        TextView view = getView();
        // Drawable loading = view.getContext().getResources().getDrawable(R.drawable.remote_image); FIXME: here
        // Drawable failed = view.getContext().getResources().getDrawable(R.drawable.remote_failed);
        final RemoteDrawable remote = new RemoteDrawable(mLoadingDrawable, mFailedDrawable);

        mImageLoader.get(source, new ImageLoader.ImageListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                remote.displayFailed();
                TextView view = getView();
                if (view != null) {
                    view.invalidate();
                }
            }

            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                if (response.getBitmap() != null) {
                    // make sure view is still valid
                    TextView view = getView();
                    if (view == null) {
                        AppLog.w(T.UTILS, "WPImageGetter view is invalid");
                        return;
                    }

                    Drawable drawable = new BitmapDrawable(view.getContext().getResources(), response.getBitmap());
                    final int oldHeight = remote.getBounds().height();
                    int maxWidth = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
                    if (mMaxSize > 0 && (maxWidth > mMaxSize || maxWidth == 0)) {
                        maxWidth = mMaxSize;
                    }
                    remote.setRemoteDrawable(drawable, maxWidth);

                    // image is from cache? don't need to modify view height
                    if (isImmediate) {
                        return;
                    }

                    int newHeight = remote.getBounds().height();
                    view.invalidate();
                    // For ICS
                    view.setHeight(view.getHeight() + newHeight - oldHeight);
                    // Pre ICS
                    view.setEllipsize(null);
                }
            }
        });
        return remote;
    }

    private static class RemoteDrawable extends BitmapDrawable {
        protected Drawable mRemoteDrawable;
        protected Drawable mLoadingDrawable;
        protected Drawable mFailedDrawable;
        private boolean mDidFail = false;

        public RemoteDrawable(Drawable loadingDrawable, Drawable failedDrawable) {
            mLoadingDrawable = loadingDrawable;
            mFailedDrawable = failedDrawable;
            setBounds(0, 0, mLoadingDrawable.getIntrinsicWidth(), mLoadingDrawable.getIntrinsicHeight());
        }

        public void displayFailed() {
            mDidFail = true;
        }

        public void setBounds(int x, int y, int width, int height) {
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

        public void setRemoteDrawable(Drawable remote) {
            mRemoteDrawable = remote;
            setBounds(0, 0, mRemoteDrawable.getIntrinsicWidth(), mRemoteDrawable.getIntrinsicHeight());
        }

        public void setRemoteDrawable(Drawable remote, int maxWidth) {
            // null sentinel for now
            if (remote == null) {
                // throw error
                return;
            }
            mRemoteDrawable = remote;
            // determine if we need to scale the image to fit in view
            int imgWidth = remote.getIntrinsicWidth();
            int imgHeight = remote.getIntrinsicHeight();
            float xScale = (float) imgWidth / (float) maxWidth;
            if (xScale > 1.0f) {
                setBounds(0, 0, Math.round(imgWidth / xScale), Math.round(imgHeight / xScale));
            } else {
                setBounds(0, 0, imgWidth, imgHeight);
            }
        }

        public boolean didFail() {
            return mDidFail;
        }

        public void draw(Canvas canvas) {
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
