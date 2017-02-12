package org.wordpress.android.ui.posts.photochooser;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.ui.posts.photochooser.PhotoChooserFragment.PhotoChooserIcon;

import java.util.ArrayList;

public class PhotoChooserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private class PhotoChooserItem {
        private long _id;
        private Uri imageUri;
        private boolean isSelected;
    }

    private final Context mContext;
    private final int mImageWidth;
    private final int mImageHeight;

    private boolean mIsMultiSelectEnabled;

    private final ThumbnailLoader mThumbnailLoader;
    private final PhotoChooserFragment.OnPhotoChosenListener mListener;
    private final ArrayList<PhotoChooserItem> mPhotoList = new ArrayList<>();

    private static final int VT_PHOTO   = 0;
    private static final int VT_CAMERA  = 1;
    private static final int VT_PICKER  = 2;
    private static final int VT_WPMEDIA = 3;
    private static final int VT_EMPTY   = 4;

    private static final int NUM_NON_PHOTO_ITEMS = 3;

    public PhotoChooserAdapter(Context context,
                               int imageWidth,
                               int imageHeight,
                               PhotoChooserFragment.OnPhotoChosenListener listener) {
        super();
        mContext = context;
        mListener = listener;
        mImageWidth = imageWidth;
        mImageHeight = imageHeight;
        mThumbnailLoader = new ThumbnailLoader(context);
    }

    void loadDevicePhotos() {
        new BuildDevicePhotoListTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public int getItemCount() {
        if (hasPhotos()) {
            return mPhotoList.size() + NUM_NON_PHOTO_ITEMS;
        } else {
            return NUM_NON_PHOTO_ITEMS + 1; // +1 for VT_EMPTY
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
        } else if (position == 2) {
            return VT_WPMEDIA;
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
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        switch (viewType) {
            case VT_CAMERA:
                view = inflater.inflate(R.layout.photo_chooser_icon, parent, false);
                return new IconViewHolder(view, PhotoChooserIcon.ANDROID_CAMERA);
            case VT_PICKER:
                view = inflater.inflate(R.layout.photo_chooser_icon, parent, false);
                return new IconViewHolder(view, PhotoChooserIcon.ANDROID_PICKER);
            case VT_WPMEDIA:
                view = inflater.inflate(R.layout.photo_chooser_icon, parent, false);
                return new IconViewHolder(view, PhotoChooserIcon.WP_MEDIA);
            case VT_EMPTY:
                view = inflater.inflate(R.layout.photo_chooser_empty, parent, false);
                return new EmptyViewHolder(view);
            default:
                view = inflater.inflate(R.layout.photo_chooser_thumbnail, parent, false);
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
            PhotoChooserItem item = getPhotoItemAtPosition(position);
            PhotoViewHolder photoHolder = (PhotoViewHolder) holder;
            photoHolder.selectedFrame.setVisibility(item.isSelected ? View.VISIBLE : View.GONE);
            mThumbnailLoader.loadThumbnail(photoHolder.imageView, item._id);
        }
    }

    void setMultiSelectEnabled(boolean enabled) {
        if (mIsMultiSelectEnabled == enabled) return;

        mIsMultiSelectEnabled = enabled;

        // clear existing selection when multi-select is disabled
        if (!enabled) {
            boolean anyChanged = false;
            for (PhotoChooserItem item : mPhotoList) {
                if (item.isSelected) {
                    item.isSelected = false;
                    anyChanged = true;
                }
            }
            if (anyChanged) {
                notifyDataSetChanged();
            }
        }
    }

    boolean isMultiSelectEnabled() {
        return mIsMultiSelectEnabled;
    }

    void togglePhotoSelection(Uri imageUri) {
        int photoIndex = indexOfImageUri(imageUri);
        if (photoIndex > -1) {
            mPhotoList.get(photoIndex).isSelected = !mPhotoList.get(photoIndex).isSelected;
            int adapterIndex = photoIndex + NUM_NON_PHOTO_ITEMS;
            notifyItemChanged(adapterIndex);
        }
    }

    ArrayList<Uri> getSelectedImageURIs() {
        ArrayList<Uri> uriList = new ArrayList<>();
        for (PhotoChooserItem item: mPhotoList) {
            if (item.isSelected) {
                uriList.add(item.imageUri);
            }
        }
        return uriList;
    }

    void setSelectedImageURIs(ArrayList<Uri> uriList) {
        boolean anyChanged = false;

        // first clear any existing selection
        for (PhotoChooserItem item: mPhotoList) {
            if (item.isSelected) {
                item.isSelected = false;
                anyChanged = true;
            }
        }

        // then select the passed images
        for (Uri imageUri: uriList) {
            int photoIndex = indexOfImageUri(imageUri);
            if (photoIndex > -1) {
                mPhotoList.get(photoIndex).isSelected = true;
                anyChanged = true;
            }
        }
        if (anyChanged) {
            notifyDataSetChanged();
        }
    }

    private int indexOfImageUri(Uri imageUri) {
        for (int i = 0; i < mPhotoList.size(); i++) {
            if (mPhotoList.get(i).imageUri.equals(imageUri)) {
                return i;
            }
        }
        return -1;
    }

    /*
     * ViewHolder containing a device photo
     */
    class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final View selectedFrame;
        private final GestureDetector detector;

        public PhotoViewHolder(View view) {
            super(view);

            selectedFrame = view.findViewById(R.id.selected_frame);
            selectedFrame.getLayoutParams().width = mImageWidth;
            selectedFrame.getLayoutParams().height = mImageHeight;

            imageView = (ImageView) view.findViewById(R.id.image_photo);
            imageView.getLayoutParams().width = mImageWidth;
            imageView.getLayoutParams().height = mImageHeight;

            detector = new GestureDetector(view.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (mListener != null) {
                        Uri imageUri = getPhotoItemAtPosition(getAdapterPosition()).imageUri;
                        mListener.onPhotoTapped(imageUri);
                    }
                    return true;
                }
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (mListener != null) {
                        Uri imageUri = getPhotoItemAtPosition(getAdapterPosition()).imageUri;
                        mListener.onPhotoDoubleTapped(imageUri);
                    }
                    return true;
                }
                @Override
                public void onLongPress(MotionEvent e) {
                    if (mListener != null) {
                        Uri imageUri = getPhotoItemAtPosition(getAdapterPosition()).imageUri;
                        mListener.onPhotoLongPressed(imageUri);
                    }
                }
            });

            imageView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    detector.onTouchEvent(event);
                    return true;
                }
            });
        }
    }

    /*
     * ViewHolder containing the camera, picker, or wp media icon
     */
    class IconViewHolder extends RecyclerView.ViewHolder {
        public IconViewHolder(View view, final PhotoChooserIcon icon) {
            super(view);

            itemView.getLayoutParams().width = mImageWidth;
            itemView.getLayoutParams().height = mImageHeight;

            ImageView imgIcon = (ImageView) view.findViewById(R.id.image_icon);
            switch (icon) {
                case ANDROID_CAMERA:
                    imgIcon.setImageResource(R.drawable.camera);
                    break;
                case ANDROID_PICKER:
                    imgIcon.setImageResource(R.drawable.ic_collections_48px);
                    break;
                case WP_MEDIA:
                    // TODO: need small black WP media icon
                    imgIcon.setImageResource(R.drawable.nux_icon_wp);
                    break;
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onIconClicked(icon);
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
     * builds the list of PhotoChooserItems from the device
     */
    private class BuildDevicePhotoListTask extends AsyncTask<Void, Void, Boolean> {
        private final ArrayList<PhotoChooserItem> tmpList = new ArrayList<>();
        private static final String ID_COL = MediaStore.Images.Media._ID;

        @Override
        protected Boolean doInBackground(Void... params) {
            String[] projection = { ID_COL };
            String orderBy = ID_COL + " DESC";

            // get external (SDCARD) images
            Cursor external = mContext.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    orderBy);
            addImages(external, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            // get internal images
            Cursor internal = mContext.getContentResolver().query(
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    orderBy);
            addImages(internal, MediaStore.Images.Media.INTERNAL_CONTENT_URI);

            return true;
        }

        private void addImages(Cursor cursor, Uri baseUri) {
            int idIndex = cursor.getColumnIndexOrThrow(ID_COL);
            while (cursor.moveToNext()) {
                PhotoChooserItem item = new PhotoChooserItem();
                item._id = cursor.getLong(idIndex);
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
