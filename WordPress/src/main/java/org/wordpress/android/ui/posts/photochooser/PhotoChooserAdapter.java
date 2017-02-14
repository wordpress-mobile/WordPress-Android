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
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class PhotoChooserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private class PhotoChooserItem {
        private long _id;
        private Uri uri;
        private boolean isVideo;
    }

    private class UriList extends ArrayList<Uri> {
        private int indexOfUri(Uri imageUri) {
            for (int i = 0; i < size(); i++) {
                if (get(i).equals(imageUri)) {
                    return i;
                }
            }
            return -1;
        }
    }

    private final UriList mSelectedUris = new UriList();

    private final Context mContext;
    private final int mThumbWidth;
    private final int mThumbHeight;

    private boolean mIsMultiSelectEnabled;

    private final ThumbnailLoader mThumbnailLoader;
    private final PhotoChooserFragment.OnPhotoChooserListener mListener;
    private final ArrayList<PhotoChooserItem> mMediaList = new ArrayList<>();

    private static final int VT_PHOTO   = 0;
    private static final int VT_EMPTY   = 1;

    public PhotoChooserAdapter(Context context,
                               int thumbWidth,
                               int thumbHeight,
                               PhotoChooserFragment.OnPhotoChooserListener listener) {
        super();
        mContext = context;
        mListener = listener;
        mThumbWidth = thumbWidth;
        mThumbHeight = thumbHeight;
        setHasStableIds(true);
        mThumbnailLoader = new ThumbnailLoader(context);
    }

    void loadDeviceMedia() {
        new BuildDeviceMediaListTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public int getItemCount() {
        if (hasPhotos()) {
            return mMediaList.size();
        } else {
            return 1; // single VT_EMPTY cell
        }
    }

    @Override
    public long getItemId(int position) {
        int type = getItemViewType(position);
        if (type == VT_PHOTO) {
            return getItemAtPosition(position)._id;
        } else {
            return type;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (hasPhotos()) {
            return VT_PHOTO;
        } else {
            return VT_EMPTY;
        }
    }

    private boolean hasPhotos() {
        return mMediaList.size() > 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        switch (viewType) {
            case VT_EMPTY:
                view = inflater.inflate(R.layout.photo_chooser_empty, parent, false);
                return new EmptyViewHolder(view);
            default:
                view = inflater.inflate(R.layout.photo_chooser_thumbnail, parent, false);
                return new ThumbnailViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ThumbnailViewHolder) {
            PhotoChooserItem item = getItemAtPosition(position);
            ThumbnailViewHolder thumbHolder = (ThumbnailViewHolder) holder;

            int selectedIndex = mSelectedUris.indexOfUri(item.uri);
            if (selectedIndex > -1) {
                thumbHolder.selectedFrame.setVisibility(View.VISIBLE);
                thumbHolder.txtSelectionCount.setVisibility(View.VISIBLE);
                thumbHolder.txtSelectionCount.setText(Integer.toString(selectedIndex + 1));
            } else {
                thumbHolder.selectedFrame.setVisibility(View.GONE);
                thumbHolder.txtSelectionCount.setVisibility(View.GONE);
            }

            thumbHolder.videoOverlay.setVisibility(item.isVideo ? View.VISIBLE : View.GONE);
            mThumbnailLoader.loadThumbnail(thumbHolder.imgThumbnail, item._id, item.isVideo);
        }
    }

    private PhotoChooserItem getItemAtPosition(int position) {
        if (!isValidPosition(position)) {
            AppLog.w(AppLog.T.POSTS, "photo chooser > invalid position in getItemAtPosition");
            return null;
        }
        return mMediaList.get(position);
    }

    boolean isVideoUri(Uri uri) {
        int index = indexOfUri(uri);
        return index > -1 ? getItemAtPosition(index).isVideo : false;
    }

    void setMultiSelectEnabled(boolean enabled) {
        if (mIsMultiSelectEnabled == enabled) return;

        mIsMultiSelectEnabled = enabled;

        if (!enabled && getNumSelected() > 0) {
            mSelectedUris.clear();
            notifyDataSetChangedNoFade();
        }
    }

    boolean isMultiSelectEnabled() {
        return mIsMultiSelectEnabled;
    }

    /*
     * calls notifyDataSetChanged() with the ThumbnailLoader image fade disabled - used to
     * prevent unnecessary flicker when changing existing items
     */
    private void notifyDataSetChangedNoFade() {
        mThumbnailLoader.temporarilyDisableFade();
        notifyDataSetChanged();
    }

    // TODO: should there be a limit to the number of items the user can select?
    void toggleSelection(Uri uri) {
        if (indexOfUri(uri) == -1) return;

        int selectedIndex = mSelectedUris.indexOfUri(uri);
        if (selectedIndex > -1) {
            mSelectedUris.remove(selectedIndex);
        } else {
            mSelectedUris.add(uri);
        }

        notifyDataSetChangedNoFade();
    }

    private boolean isValidPosition(int position) {
        return position >= 0 && position < mMediaList.size();
    }

    /*
     * scales in/out the selection count depending on whether the item is selected
     */
    private void animateSelectionCount(ThumbnailViewHolder holder, int position) {
        if (!isValidPosition(position)) {
            AppLog.w(AppLog.T.POSTS, "photo chooser > invalid position in animateSelectionCount");
            return;
        }

        boolean isSelected = mSelectedUris.contains(mMediaList.get(position).uri);
        AniUtils.startAnimation(holder.txtSelectionCount,
                isSelected ? R.anim.cab_select : R.anim.cab_deselect);
        holder.txtSelectionCount.setVisibility(isSelected ? View.VISIBLE : View.GONE);
    }


    ArrayList<Uri> getSelectedURIs() {
        return mSelectedUris;
    }

    void setSelectedURIs(ArrayList<Uri> uriList) {
        mSelectedUris.clear();
        mSelectedUris.addAll(uriList);
        notifyDataSetChangedNoFade();
    }

    int getNumSelected() {
        return mIsMultiSelectEnabled ? mSelectedUris.size() : 0;
    }

    private int indexOfUri(Uri uri) {
        for (int i = 0; i < mMediaList.size(); i++) {
            if (mMediaList.get(i).uri.equals(uri)) {
                return i;
            }
        }
        return -1;
    }

    /*
     * ViewHolder containing a device thumbnail
     */
    class ThumbnailViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgThumbnail;
        private final View selectedFrame;
        private final TextView txtSelectionCount;
        private final View videoOverlay;
        private final GestureDetector detector;

        public ThumbnailViewHolder(View view) {
            super(view);

            imgThumbnail = (ImageView) view.findViewById(R.id.image_thumbnail);
            selectedFrame = view.findViewById(R.id.selected_frame);
            txtSelectionCount = (TextView) view.findViewById(R.id.text_selection_count);
            videoOverlay = view.findViewById(R.id.image_video_overlay);

            selectedFrame.getLayoutParams().width = mThumbWidth;
            selectedFrame.getLayoutParams().height = mThumbHeight;

            imgThumbnail.getLayoutParams().width = mThumbWidth;
            imgThumbnail.getLayoutParams().height = mThumbHeight;

            detector = new GestureDetector(view.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    int position = getAdapterPosition();
                    if (mListener != null) {
                        Uri imageUri = getItemAtPosition(position).uri;
                        mListener.onPhotoTapped(itemView, imageUri);
                    }
                    if (isMultiSelectEnabled()) {
                        animateSelectionCount(ThumbnailViewHolder.this, position);
                    }
                    return true;
                }
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    int position = getAdapterPosition();
                    if (mListener != null) {
                        Uri imageUri = getItemAtPosition(position).uri;
                        mListener.onPhotoDoubleTapped(itemView, imageUri);
                    }
                    return true;
                }
                @Override
                public void onLongPress(MotionEvent e) {
                    int position = getAdapterPosition();
                    if (mListener != null) {
                        Uri imageUri = getItemAtPosition(position).uri;
                        mListener.onPhotoLongPressed(itemView, imageUri);
                    }
                }
            });

            imgThumbnail.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    detector.onTouchEvent(event);
                    return true;
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
            itemView.getLayoutParams().width = mThumbWidth;
            itemView.getLayoutParams().height = mThumbHeight;
        }
    }

    /*
     * builds the list of PhotoChooserItems from the device
     */
    private class BuildDeviceMediaListTask extends AsyncTask<Void, Void, Boolean> {
        private final ArrayList<PhotoChooserItem> tmpList = new ArrayList<>();
        private static final String ID_COL = MediaStore.Images.Media._ID;

        @Override
        protected Boolean doInBackground(Void... params) {
            // external (SDCARD) images
            addMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false);

            // internal images
            addMedia(MediaStore.Images.Media.INTERNAL_CONTENT_URI, false);

            // external videos
            addMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true);

            // internal videos
            addMedia(MediaStore.Video.Media.INTERNAL_CONTENT_URI, true);

            // sort by id in reverse (newest first)
            Collections.sort(tmpList, new Comparator<PhotoChooserItem>() {
                @Override
                public int compare(PhotoChooserItem o1, PhotoChooserItem o2) {
                    long id1 = o1._id;
                    long id2 = o2._id;
                    return (id2 < id1) ? -1 : ((id1 == id2) ? 0 : 1);
                }
            });

            return true;
        }

        private void addMedia(Uri baseUri, boolean isVideo) {
            String[] projection = { ID_COL };
            Cursor cursor = mContext.getContentResolver().query(
                    baseUri,
                    projection,
                    null,
                    null,
                    null);
            if (cursor == null) {
                return;
            }

            int idIndex = cursor.getColumnIndexOrThrow(ID_COL);
            while (cursor.moveToNext()) {
                PhotoChooserItem item = new PhotoChooserItem();
                item._id = cursor.getLong(idIndex);
                item.uri = Uri.withAppendedPath(baseUri, "" + item._id);
                item.isVideo = isVideo;
                tmpList.add(item);
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            mMediaList.clear();
            mMediaList.addAll(tmpList);
            notifyDataSetChanged();
        }
    }
}
