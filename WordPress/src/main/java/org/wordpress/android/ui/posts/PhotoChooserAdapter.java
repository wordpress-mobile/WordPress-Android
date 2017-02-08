package org.wordpress.android.ui.posts;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.ui.posts.PhotoChooserFragment.OnPhotoChosenListener;

import java.util.ArrayList;

public class PhotoChooserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private final int mImageSz;
    private final OnPhotoChosenListener mListener;
    private final ArrayList<Uri> mUriList = new ArrayList<>();

    private static final int VT_CAMERA = 1;
    private static final int VT_PHOTO = 2;

    public PhotoChooserAdapter(Context context,
                               int imageSize,
                               OnPhotoChosenListener listener) {
        super();
        mContext = context;
        mListener = listener;
        mImageSz = imageSize;
    }

    private static final String ID_COL = MediaStore.Images.Thumbnails._ID;
    private static final String IMAGE_ID_COL = MediaStore.Images.Thumbnails.IMAGE_ID;

    void loadDevicePhotos() {
        new LoadDevicePhotosTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public int getItemCount() {
        return mUriList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VT_CAMERA;
        }
        return VT_PHOTO;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VT_CAMERA:
                View cameraView = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_chooser_camera, parent, false);
                return new CameraViewHolder(cameraView);
            default:
                View photoView = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_chooser_image, parent, false);
                return new PhotoViewHolder(photoView);
        }
    }

    /*
     * returns the Uri of the photo in the adapter at the passed position
     */
    private Uri getPhotoAtPosition(int adapterPosition) {
        // -1 to take initial VT_CAMERA item into account
        int photoPosition = adapterPosition - 1;
        return mUriList.get(photoPosition);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PhotoViewHolder) {
            ((PhotoViewHolder) holder).imageView.setImageURI(getPhotoAtPosition(position));
        }
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        public PhotoViewHolder(View view) {
            super(view);

            imageView = (ImageView) view.findViewById(R.id.image_photo);
            imageView.getLayoutParams().width = mImageSz;
            imageView.getLayoutParams().height = mImageSz;

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        int position = getAdapterPosition();
                        mListener.onPhotoChosen(getPhotoAtPosition(position));
                    }
                }
            });
        }
    }

    class CameraViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgCamera;

        public CameraViewHolder(View view) {
            super(view);

            imgCamera = (ImageView) view.findViewById(R.id.image_camera);
            imgCamera.getLayoutParams().width = mImageSz;
            imgCamera.getLayoutParams().height = mImageSz;

            imgCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onCameraChosen();
                    }
                }
            });
        }
    }

    private class LoadDevicePhotosTask extends AsyncTask<Void, Void, Boolean> {
        private final ArrayList<Uri> tmpUriList = new ArrayList<>();

        @Override
        protected Boolean doInBackground(Void... params) {
            String[] projection = { ID_COL, IMAGE_ID_COL };
            String orderBy = IMAGE_ID_COL + " DESC";

            // get external (SDCARD) images
            Cursor external = mContext.getContentResolver().query(
                    MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    orderBy);
            addImages(external, MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI);

            // get internal images
            Cursor internal = mContext.getContentResolver().query(
                    MediaStore.Images.Thumbnails.INTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    orderBy);
            addImages(internal, MediaStore.Images.Thumbnails.INTERNAL_CONTENT_URI);

            return true;
        }

        private void addImages(Cursor cursor, Uri baseUri) {
            int index = cursor.getColumnIndexOrThrow(ID_COL);
            while (cursor.moveToNext()) {
                int imageID = cursor.getInt(index);
                Uri imageUri = Uri.withAppendedPath(baseUri, "" + imageID);
                tmpUriList.add(imageUri);
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            mUriList.clear();
            mUriList.addAll(tmpUriList);
            notifyDataSetChanged();
        }
    }
}
