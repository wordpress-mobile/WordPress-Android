package org.wordpress.mediapicker.source;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Parcel;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.mediapicker.MediaItem;
import org.wordpress.mediapicker.MediaUtils;
import org.wordpress.mediapicker.R;

import java.util.ArrayList;
import java.util.List;

public class MediaSourceCaptureImage implements MediaSource, Camera.PictureCallback {
    private final static String HAS_PICTURE_KEY = "has-taken-picture";

    private final MediaItem mMediaItem;

    // Deprecated in API 21 but not included in the support library, update when (min API >= 21)
    private CameraPreview mCameraPreview;
    private CameraPreview mFullscreenPreview;

    private Context mContext;
    private boolean mIsCaptureReady;
    private boolean mHasTakenPicture;

    // Captured images
    private List<MediaItem> mCapturedImages = new ArrayList<>();

    public MediaSourceCaptureImage() {
        mMediaItem = new MediaItem();
        mIsCaptureReady = false;
        mHasTakenPicture = false;
        mCameraPreview = null;
    }

    @Override
    public void setListener(OnMediaChange listener) {
    }

    @Override
    public int getCount() {
        return 1 + mCapturedImages.size();
    }

    @Override
    public MediaItem getMedia(int position) {
        if (position > 0) {
            return mCapturedImages.get(position - 1);
        }

        return mMediaItem;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent, LayoutInflater inflater, final ImageLoader.ImageCache cache) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.media_item_capture, parent, false);
        }

        mContext = inflater.getContext();

        if (position > 0) {
            final ImageView overlayView = (ImageView) convertView.findViewById(R.id.capture_view_overlay);
            if (overlayView != null) {
                int width = overlayView.getWidth();
                int height = overlayView.getHeight();

                if (width <= 0 || height <= 0) {
                    overlayView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            int width = overlayView.getWidth();
                            int height = overlayView.getHeight();
                            setImage(mCapturedImages.get(position - 1).getSource(), cache, overlayView, mCapturedImages.get(position - 1), width, height);
                            overlayView.getViewTreeObserver().removeOnPreDrawListener(this);
                            return true;
                        }
                    });
                } else {
                    setImage(mCapturedImages.get(position - 1).getSource(), cache, overlayView, mCapturedImages.get(position - 1), width, height);
                }
            }
        } else {
            if (mCameraPreview == null) {
                mCameraPreview = (CameraPreview) convertView.findViewById(R.id.capture_view_preview);
                mCameraPreview.setHighRes(false);
                mCameraPreview.setCamera(getBackCameraId());
            }

            ImageView overlayView = (ImageView) convertView.findViewById(R.id.capture_view_overlay);

            if (overlayView != null) {
                overlayView.setImageResource(getOverlayResource());
            }
        }

        return convertView;
    }

    private void setImage(Uri imageSource, ImageLoader.ImageCache cache, ImageView imageView, MediaItem mediaItem, int width, int height) {
        if (imageSource != null) {
            Bitmap imageBitmap = null;
            if (cache != null) {
                imageBitmap = cache.getBitmap(imageSource.toString());
            }

            if (imageBitmap == null) {
                imageView.setImageResource(R.drawable.media_item_placeholder);
                MediaUtils.BackgroundFetchThumbnail bgDownload =
                        new MediaUtils.BackgroundFetchThumbnail(imageView,
                                cache,
                                MediaUtils.BackgroundFetchThumbnail.TYPE_IMAGE,
                                width,
                                height,
                                mediaItem.getRotation());
                imageView.setTag(bgDownload);
                bgDownload.execute(imageSource);
            } else {
                MediaUtils.fadeInImage(imageView, imageBitmap);
            }
        } else {
            imageView.setTag(null);
            imageView.setImageResource(R.drawable.media_item_placeholder);
        }
    }

    @Override
    public boolean onMediaItemSelected(final MediaItem mediaItem, boolean selected) {
        if (!mIsCaptureReady) {
            final int cameraId = mCameraPreview.getCameraId();
            mCameraPreview.releaseCamera();

            // TODO: necessary?
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFullscreenPreview = new CameraPreview(mContext);
                    mFullscreenPreview.setHighRes(true);
                    mFullscreenPreview.setCanTakePicture(true);
                    mFullscreenPreview.setCameraCallbacks(null, null, MediaSourceCaptureImage.this);
                    mFullscreenPreview.setCamera(cameraId);
                }
            });

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            ((Activity) mContext).addContentView(mFullscreenPreview, params);

            mCameraPreview.setVisibility(View.INVISIBLE);
            mIsCaptureReady = true;

            return true;
        }

        return false;
    }

    protected int getOverlayResource() {
        return R.drawable.camera;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if (mContext != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 5;

            // sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imageAdded)));
            Bitmap imageBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            String url = MediaStore.Images.Media.insertImage(mContext.getContentResolver(), imageBitmap, "camera-image.jpg", "description");

            MediaItem capturedItem = new MediaItem();
            capturedItem.setSource(url);
            mCapturedImages.add(capturedItem);

            mHasTakenPicture = true;
            mIsCaptureReady = false;

            mFullscreenPreview.releaseCamera();
            mFullscreenPreview.setVisibility(View.INVISIBLE);

            ((ViewGroup) mFullscreenPreview.getParent()).removeView(mFullscreenPreview);

            mCameraPreview.setVisibility(View.VISIBLE);
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCameraPreview.setCamera(getBackCameraId());
                    mCameraPreview.prepareCamera();
                    mCameraPreview.startCameraPreview(mCameraPreview.getHolder());
                }
            });
        }
    }

    /**
     * Helper method; determines ID of back-facing camera
     *
     * @return
     *  back-facing camera ID or -1 if none could be found
     */
    private int getBackCameraId() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();

        for (int cameraId = 0; cameraId < cameraCount; ++cameraId) {
            Camera.getCameraInfo(cameraId, cameraInfo);

            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return cameraId;
            }
        }

        return -1;
    }

    /*
        Parcelable interface
     */
    public static final Creator<MediaSourceCaptureImage> CREATOR =
            new Creator<MediaSourceCaptureImage>() {
                public MediaSourceCaptureImage createFromParcel(Parcel in) {
                    return new MediaSourceCaptureImage();
                }

                public MediaSourceCaptureImage[] newArray(int size) {
                    return new MediaSourceCaptureImage[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mHasTakenPicture) {
            dest.writeString(HAS_PICTURE_KEY);
        }

        if (mCameraPreview != null) {
            mCameraPreview.releaseCamera();
        }

        // TODO: save captured images
    }
}
