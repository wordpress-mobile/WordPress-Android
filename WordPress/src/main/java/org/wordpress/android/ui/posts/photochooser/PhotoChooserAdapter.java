package org.wordpress.android.ui.posts.photochooser;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.wordpress.android.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class PhotoChooserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private class PhotoChooserItem {
        private long _id;
        private long imageId;
        private Uri imageUri;
    }

    private final Context mContext;
    private final int mImageWidth;
    private final int mImageHeight;
    private final PhotoChooserFragment.OnPhotoChosenListener mListener;
    private final ArrayList<PhotoChooserItem> mPhotoList = new ArrayList<>();

    private static final int VT_PHOTO  = 0;
    private static final int VT_CAMERA = 1;
    private static final int VT_PICKER = 2;
    private static final int VT_EMPTY  = 3;

    private static final int NUM_NON_PHOTO_ITEMS = 2;

    public PhotoChooserAdapter(Context context,
                               int imageWidth,
                               int imageHeight,
                               PhotoChooserFragment.OnPhotoChosenListener listener) {
        super();
        mContext = context;
        mListener = listener;
        mImageWidth = imageWidth;
        mImageHeight = imageHeight;
    }

    private static final String ID_COL = MediaStore.Images.Thumbnails._ID;
    private static final String IMAGE_ID_COL = MediaStore.Images.Thumbnails.IMAGE_ID;

    void loadDevicePhotos() {
        new BuildDevicePhotoListTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public int getItemCount() {
        if (hasPhotos()) {
            return mPhotoList.size() + NUM_NON_PHOTO_ITEMS;
        } else {
            return NUM_NON_PHOTO_ITEMS + 1;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VT_CAMERA;
        } else if (position == 1) {
            return VT_PICKER;
        } else if (hasPhotos()) {
            return VT_PHOTO;
        } else {
            return VT_EMPTY;
        }
    }

    private boolean hasPhotos() {
        return mPhotoList.size() > 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VT_CAMERA:
                view = inflater.inflate(R.layout.photo_chooser_camera, parent, false);
                return new CameraViewHolder(view);
            case VT_PICKER:
                view = inflater.inflate(R.layout.photo_chooser_picker, parent, false);
                return new PickerViewHolder(view);
            case VT_EMPTY:
                view = inflater.inflate(R.layout.photo_chooser_empty, parent, false);
                return new EmptyViewHolder(view);
            default:
                view = inflater.inflate(R.layout.photo_chooser_image, parent, false);
                return new PhotoViewHolder(view);
        }
    }

    /*
     * returns the photo item in the adapter at the passed position
     */
    private PhotoChooserItem getPhotoItemAtPosition(int adapterPosition) {
        // take initial VT_CAMERA and VT_PICKER items into account
        int photoPosition = adapterPosition - NUM_NON_PHOTO_ITEMS;
        return mPhotoList.get(photoPosition);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PhotoViewHolder) {
            ImageView imageView = ((PhotoViewHolder) holder).imageView;
            PhotoChooserItem item = getPhotoItemAtPosition(position);
            new ImageLoaderTask(imageView, item.imageId, item.imageUri)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /*
     * ViewHolder containing a device photo
     */
    class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        public PhotoViewHolder(View view) {
            super(view);

            imageView = (ImageView) view.findViewById(R.id.image_photo);
            imageView.getLayoutParams().width = mImageWidth;
            imageView.getLayoutParams().height = mImageHeight;

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        Uri imageUri = getPhotoItemAtPosition(getAdapterPosition()).imageUri;
                        mListener.onPhotoChosen(imageUri);
                    }
                }
            });
        }
    }

    /*
     * ViewHolder containing the camera icon
     */
    class CameraViewHolder extends RecyclerView.ViewHolder {
        public CameraViewHolder(View view) {
            super(view);

            itemView.getLayoutParams().width = mImageWidth;
            itemView.getLayoutParams().height = mImageHeight;

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onIconClicked(PhotoChooserFragment.PhotoChooserIcon.ANDROID_CAMERA);
                    }
                }
            });
        }
    }

    /*
     * ViewHolder containing the stock picker icon
     */
    class PickerViewHolder extends RecyclerView.ViewHolder {
        public PickerViewHolder(View view) {
            super(view);

            itemView.getLayoutParams().width = mImageWidth;
            itemView.getLayoutParams().height = mImageHeight;

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onIconClicked(PhotoChooserFragment.PhotoChooserIcon.ANDROID_PICKER);
                    }
                }
            });
        }
    }

    /*
     * ViewHolder containing the message that appears when there are no device photos
     */
    class EmptyViewHolder extends RecyclerView.ViewHolder {
        public EmptyViewHolder(View view) {
            super(view);
            itemView.getLayoutParams().width = mImageWidth;
            itemView.getLayoutParams().height = mImageHeight;
        }
    }

    /*
     * task to load a device thumbnail and display it in an ImageView
     */
    private class ImageLoaderTask extends AsyncTask<Void, Void, Boolean> {
        private final WeakReference<ImageView> mWeakImageView;
        private final Uri mImageUri;
        private final long mImageId;
        private Bitmap mBitmap;

        ImageLoaderTask(ImageView imageView, long imageId, Uri imageUri) {
            imageView.setImageResource(R.drawable.photo_chooser_item_background);
            mWeakImageView = new WeakReference<>(imageView);
            mImageId = imageId;
            mImageUri = imageUri;
            imageView.setTag(imageUri.toString());
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mBitmap = MediaStore.Images.Thumbnails.getThumbnail(
                    mContext.getContentResolver(),
                    mImageId,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null);
            return mBitmap != null;
        }

        private boolean isImageViewValid() {
            ImageView imageView = mWeakImageView.get();
            if (imageView != null && imageView.getTag() instanceof String) {
                // make sure this imageView is still tagged with it's initial image Uri - it may
                // be different if the view was recycled
                String requestedUri = mImageUri.toString();
                String taggedUri = (String) imageView.getTag();
                return taggedUri.equals(requestedUri);
            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result && isImageViewValid()) {
                mWeakImageView.get().setImageBitmap(mBitmap);
            }
        }
    }

    /*
     * builds the list of PhotoChooserItems from the device
     */
    private class BuildDevicePhotoListTask extends AsyncTask<Void, Void, Boolean> {
        private final ArrayList<PhotoChooserItem> tmpList = new ArrayList<>();

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
            int idIndex = cursor.getColumnIndexOrThrow(ID_COL);
            int imageIdIndex = cursor.getColumnIndexOrThrow(IMAGE_ID_COL);
            while (cursor.moveToNext()) {
                PhotoChooserItem item = new PhotoChooserItem();
                item._id = cursor.getLong(idIndex);
                item.imageId = cursor.getLong(imageIdIndex);
                item.imageUri = Uri.withAppendedPath(baseUri, "" + item._id);
                tmpList.add(item);
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            mPhotoList.clear();
            mPhotoList.addAll(tmpList);
            notifyDataSetChanged();
        }
    }
}
