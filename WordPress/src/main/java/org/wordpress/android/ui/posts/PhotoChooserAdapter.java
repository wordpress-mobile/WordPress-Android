package org.wordpress.android.ui.posts;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import org.wordpress.android.ui.posts.PhotoChooserFragment.PhotoChooserIcon;
import org.wordpress.android.util.AniUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class PhotoChooserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private class PhotoItem {
        private long _id;
        private long imageId;
        private Uri imageUri;
    }
    private class PhotoItemList extends ArrayList<PhotoItem> {

    }

    private final Context mContext;
    private final int mImageWidth;
    private final int mImageHeight;
    private final OnPhotoChosenListener mListener;
    private final PhotoItemList mPhotoList = new PhotoItemList();

    private static final int VT_PHOTO = 0;
    private static final int VT_CAMERA = 1;
    private static final int VT_PICKER = 2;

    private static final int NUM_NON_PHOTO_ITEMS = 2;

    public PhotoChooserAdapter(Context context,
                               int imageWidth,
                               int imageHeight,
                               OnPhotoChosenListener listener) {
        super();
        mContext = context;
        mListener = listener;
        mImageWidth = imageWidth;
        mImageHeight = imageHeight;
    }

    private static final String ID_COL = MediaStore.Images.Thumbnails._ID;
    private static final String IMAGE_ID_COL = MediaStore.Images.Thumbnails.IMAGE_ID;

    void loadDevicePhotos() {
        new LoadDevicePhotosTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public int getItemCount() {
        return mPhotoList.size() + NUM_NON_PHOTO_ITEMS;
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
        } else {
            return VT_PHOTO;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VT_CAMERA:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_chooser_camera, parent, false);
                return new CameraViewHolder(view);
            case VT_PICKER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_chooser_picker, parent, false);
                return new PickerViewHolder(view);
            default:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_chooser_image, parent, false);
                return new PhotoViewHolder(view);
        }
    }

    /*
     * returns the photo item in the adapter at the passed position
     */
    private PhotoItem getPhotoAtPosition(int adapterPosition) {
        // take initial VT_CAMERA and VT_PICKER items into account
        int photoPosition = adapterPosition - NUM_NON_PHOTO_ITEMS;
        return mPhotoList.get(photoPosition);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PhotoViewHolder) {
            // TODO: setImageURI() is called on the main thread which makes scrolling janky - need
            // a different solution here
            //((PhotoViewHolder) holder).imageView.setImageURI(getPhotoAtPosition(position).imageUri);
            ImageView imageView = ((PhotoViewHolder) holder).imageView;
            new ImageLoaderTask(imageView, getPhotoAtPosition(position))
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

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
                        // quickly scale out the photo then call the listener
                        final int position = getAdapterPosition();
                        AniUtils.scaleOut(v, View.VISIBLE, AniUtils.Duration.SHORT, new AniUtils.AnimationEndListener() {
                            @Override
                            public void onAnimationEnd() {
                                mListener.onPhotoChosen(getPhotoAtPosition(position).imageUri);
                            }
                        });
                    }
                }
            });
        }
    }

    class CameraViewHolder extends RecyclerView.ViewHolder {
        public CameraViewHolder(View view) {
            super(view);

            itemView.getLayoutParams().width = mImageWidth;
            itemView.getLayoutParams().height = mImageHeight;

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onIconClicked(PhotoChooserIcon.ANDROID_CAMERA);
                    }
                }
            });
        }
    }

    class PickerViewHolder extends RecyclerView.ViewHolder {
        public PickerViewHolder(View view) {
            super(view);

            itemView.getLayoutParams().width = mImageWidth;
            itemView.getLayoutParams().height = mImageHeight;

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onIconClicked(PhotoChooserIcon.ANDROID_PICKER);
                    }
                }
            });
        }
    }

    private class ImageLoaderTask extends AsyncTask<Void, Void, Boolean> {
        private final WeakReference<ImageView> mWeakImageView;
        private final PhotoItem mItem;
        private Bitmap mBitmap;

        ImageLoaderTask(ImageView imageView, PhotoItem item) {
            imageView.setImageResource(R.drawable.photo_chooser_item_background);
            mWeakImageView = new WeakReference<>(imageView);
            mItem = item;
            imageView.setTag(item.imageUri.toString());
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            BitmapFactory.Options options = null;
            mBitmap = MediaStore.Images.Thumbnails.getThumbnail(
                    mContext.getContentResolver(),
                    mItem.imageId,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    options
                    );
            return mBitmap != null;
        }

        private boolean isImageViewValid() {
            ImageView imageView = mWeakImageView.get();
            if (imageView != null && imageView.getTag() instanceof String) {
                String requestedUri = mItem.imageUri.toString();
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

    private class LoadDevicePhotosTask extends AsyncTask<Void, Void, Boolean> {
        private final PhotoItemList tmpList = new PhotoItemList();

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
                PhotoItem item = new PhotoItem();
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
