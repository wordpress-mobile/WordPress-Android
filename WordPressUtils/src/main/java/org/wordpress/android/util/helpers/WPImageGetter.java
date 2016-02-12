package org.wordpress.android.util.helpers;

import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.TextUtils;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.PhotonUtils;

import java.lang.ref.WeakReference;

/**
 * ImageGetter for Html.fromHtml()
 * adapted from existing ImageGetter code in NoteCommentFragment
 */
public class WPImageGetter implements Html.ImageGetter {
    private final WeakReference<TextView> mWeakView;
    private final int mMaxSize;
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
                if (response.getBitmap() == null) {
                    AppLog.w(T.UTILS, "WPImageGetter null bitmap");
                }

                TextView view = getView();
                if (view == null) {
                    AppLog.w(T.UTILS, "WPImageGetter view is invalid");
                    return;
                }

                int maxWidth = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
                if (mMaxSize > 0 && (maxWidth > mMaxSize || maxWidth == 0)) {
                    maxWidth = mMaxSize;
                }

                Drawable drawable = new BitmapDrawable(view.getContext().getResources(), response.getBitmap());
                remote.setRemoteDrawable(drawable, maxWidth);

                // force textView to resize correctly if image isn't cached by resetting the content
                // to itself - this way the textView will use the cached image, and resizing to
                // accommodate the image isn't necessary
                if (!isImmediate) {
                    view.setText(view.getText());
                }
            }
        });

        return remote;
    }

    public static class RemoteDrawable extends BitmapDrawable {
        Drawable mRemoteDrawable;
        final Drawable mLoadingDrawable;
        final Drawable mFailedDrawable;
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
