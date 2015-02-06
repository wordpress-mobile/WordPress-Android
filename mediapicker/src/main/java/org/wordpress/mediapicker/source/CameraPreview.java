package org.wordpress.mediapicker.source;

import android.content.Context;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class CameraPreview extends SurfaceView
                        implements SurfaceHolder.Callback, Camera.PictureCallback {
    private static final String TAG = CameraPreview.class.getName();
    private static final int INVALID_CAMERA_ID = -1;

    private int mCameraId;
    private Camera mCamera;
    private boolean mResolution;
    private boolean mCanTakePicture;
    private boolean mTakingPicture;
    private Camera.ShutterCallback mShutterCallback;
    private Camera.PictureCallback mRawCallback;
    private Camera.PictureCallback mJpegCallback;

    public CameraPreview(Context context) {
        super(context);

        mCameraId = INVALID_CAMERA_ID;
        getHolder().addCallback(this);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);

        mCameraId = INVALID_CAMERA_ID;
        getHolder().addCallback(this);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (mCanTakePicture && mCamera != null && !mTakingPicture) {
            mTakingPicture = true;
            mCamera.takePicture(mShutterCallback, mRawCallback, this);

            return true;
        }

        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // surfaceChanged is called immediately after, no need to duplicate code
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() != null) {
            stopCameraPreview();
            prepareCamera();
            startCameraPreview(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCameraPreview();
        releaseCamera();
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if (mJpegCallback != null) {
            mJpegCallback.onPictureTaken(data, camera);

            mTakingPicture = false;
        }
    }

    public void setCanTakePicture(boolean canTake) {
        mCanTakePicture = canTake;
    }

    public void setCameraCallbacks(Camera.ShutterCallback shutter, Camera.PictureCallback raw, Camera.PictureCallback jpeg) {
        mShutterCallback = shutter;
        mRawCallback = raw;
        mJpegCallback = jpeg;
    }

    public void setHighRes(boolean highRes) {
        mResolution = highRes;
    }

    public Camera getCamera() {
        return mCamera;
    }

    public int getCameraId() {
        return mCameraId;
    }

    public void setCamera(int id) {
        if (mCamera == null && mCameraId == INVALID_CAMERA_ID) {
            try {
                mCameraId = id;
                mCamera = Camera.open(id);
            } catch (RuntimeException openException) {
                mCameraId = INVALID_CAMERA_ID;
                Log.w(TAG, "Camera failed to open: " + mCameraId);
            }
        }
    }

    public void prepareCamera() {
        if (mCamera == null) {
            return;
        }

        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> localSizes = parameters.getSupportedPreviewSizes();
        Camera.Size size = localSizes.get(mResolution ? 0 : localSizes.size() - 1);
        parameters.setPreviewSize(size.width, size.height);
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);
    }

    public void releaseCamera() {
        if (mCamera != null) {
            stopCameraPreview();
            mCamera.release();
            mCamera = null;
            mCameraId = INVALID_CAMERA_ID;
        }
    }

    public void startCameraPreview(SurfaceHolder holder) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.w(TAG, "Failed to start camera preview");
            }
        }
    }

    public void stopCameraPreview() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                Log.w(TAG, "Failure stopping preview:", e);
            }
        }
    }
}
