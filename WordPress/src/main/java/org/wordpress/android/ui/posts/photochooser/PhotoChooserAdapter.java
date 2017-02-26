package org.wordpress.android.ui.posts.photochooser;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
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
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.SqlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.support.v7.widget.RecyclerView.NO_POSITION;
import static org.wordpress.android.ui.posts.photochooser.PhotoChooserFragment.NUM_COLUMNS;

class PhotoChooserAdapter extends RecyclerView.Adapter<PhotoChooserAdapter.ThumbnailViewHolder> {

    private static final float SCALE_NORMAL = 1.0f;
    private static final float SCALE_SELECTED = .85f;

    /*
     * used by this adapter to communicate with the owning fragment
     */
    protected interface PhotoChooserAdapterListener {
        void onItemTapped(Uri mediaUri);
        void onItemDoubleTapped(View view, Uri mediaUri);
        void onSelectedCountChanged(int count);
        void onAdapterLoaded(boolean isEmpty);
    }

    private class PhotoChooserItem {
        private long _id;
        private Uri uri;
        private boolean isVideo;
    }

    class UriList extends ArrayList<Uri> {
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
    private int mThumbWidth;
    private int mThumbHeight;

    private boolean mIsMultiSelectEnabled;

    private final ThumbnailLoader mThumbnailLoader;
    private final PhotoChooserAdapterListener mListener;
    private final LayoutInflater mInflater;
    private final ArrayList<PhotoChooserItem> mMediaList = new ArrayList<>();

    public PhotoChooserAdapter(Context context,
                               PhotoChooserAdapterListener listener) {
        super();
        mContext = context;
        mListener = listener;
        mInflater = LayoutInflater.from(context);
        mThumbnailLoader = new ThumbnailLoader(context);
        setHasStableIds(true);
    }

    void loadDeviceMedia() {
        int displayWidth = DisplayUtils.getDisplayPixelWidth(mContext);
        mThumbWidth = displayWidth / NUM_COLUMNS;
        mThumbHeight = (int) (mThumbWidth * 0.75f);
        new BuildDeviceMediaListTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public int getItemCount() {
        return mMediaList.size();
    }

    @Override
    public long getItemId(int position) {
        if (isValidPosition(position)) {
            return getItemAtPosition(position)._id;
        } else {
            return NO_POSITION;
        }
    }

    private boolean isEmpty() {
        return mMediaList.size() == 0;
    }

    @Override
    public ThumbnailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.photo_chooser_thumbnail, parent, false);
        return new ThumbnailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ThumbnailViewHolder holder, int position) {
        PhotoChooserItem item = getItemAtPosition(position);
        if (item == null) {
            return;
        }

        int selectedIndex = mIsMultiSelectEnabled ? mSelectedUris.indexOfUri(item.uri) : -1;
        if (selectedIndex > -1) {
            holder.txtSelectionCount.setVisibility(View.VISIBLE);
            holder.txtSelectionCount.setText(Integer.toString(selectedIndex + 1));
        } else {
            holder.txtSelectionCount.setVisibility(View.GONE);
        }

        // make sure the thumbnail scale reflects its selection state
        float scale = selectedIndex > -1 ? SCALE_SELECTED : SCALE_NORMAL;
        if (holder.imgThumbnail.getScaleX() != scale) {
            holder.imgThumbnail.setScaleX(scale);
            holder.imgThumbnail.setScaleY(scale);
        }

        holder.videoOverlay.setVisibility(item.isVideo ? View.VISIBLE : View.GONE);
        mThumbnailLoader.loadThumbnail(holder.imgThumbnail, item._id, item.isVideo);
    }

    private PhotoChooserItem getItemAtPosition(int position) {
        if (!isValidPosition(position)) {
            AppLog.w(AppLog.T.POSTS, "photo chooser > invalid position in getItemAtPosition");
            return null;
        }
        return mMediaList.get(position);
    }

    private boolean isValidPosition(int position) {
        return position >= 0 && position < mMediaList.size();
    }

    boolean isVideoUri(Uri uri) {
        int index = indexOfUri(uri);
        return index > -1 && getItemAtPosition(index).isVideo;
    }

    void setMultiSelectEnabled(boolean enabled) {
        if (mIsMultiSelectEnabled == enabled) return;

        mIsMultiSelectEnabled = enabled;

        if (!enabled && mSelectedUris.size() > 0) {
            mSelectedUris.clear();
            notifyDataSetChangedNoFade();
        }
    }

    /*
     * toggles the selection state of the item at the passed position
     */
    private void toggleSelection(ThumbnailViewHolder holder, int position) {
        PhotoChooserItem item = getItemAtPosition(position);
        if (item == null) {
            return;
        }

        boolean isSelected;
        int selectedIndex = mSelectedUris.indexOfUri(item.uri);
        if (selectedIndex > -1) {
            mSelectedUris.remove(selectedIndex);
            isSelected = false;
        } else {
            mSelectedUris.add(item.uri);
            isSelected = true;
            holder.txtSelectionCount.setText(Integer.toString(mSelectedUris.size()));
        }

        // animate the count
        AniUtils.startAnimation(holder.txtSelectionCount,
                isSelected ? R.anim.cab_select : R.anim.cab_deselect);
        holder.txtSelectionCount.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        // scale the thumbnail
        if (isSelected) {
            AniUtils.scale(holder.imgThumbnail, SCALE_NORMAL, SCALE_SELECTED, AniUtils.Duration.SHORT);
        } else {
            AniUtils.scale(holder.imgThumbnail, SCALE_SELECTED, SCALE_NORMAL, AniUtils.Duration.SHORT);
        }

        if (mListener != null) {
            mListener.onSelectedCountChanged(getNumSelected());
        }

        // redraw the grid after the scale animation completes
        long delayMs = AniUtils.Duration.SHORT.toMillis(mContext);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChangedNoFade();
            }
        }, delayMs);
    }

    @NonNull
    ArrayList<Uri> getSelectedURIs() {
        return (ArrayList)mSelectedUris.clone();
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
     * calls notifyDataSetChanged() with the ThumbnailLoader image fade disabled - used to
     * prevent unnecessary flicker when changing existing items
     */
    private void notifyDataSetChangedNoFade() {
        mThumbnailLoader.temporarilyDisableFade();
        notifyDataSetChanged();
    }

    /*
     * ViewHolder containing a device thumbnail
     */
    class ThumbnailViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgThumbnail;
        private final TextView txtSelectionCount;
        private final View videoOverlay;
        private final GestureDetector detector;

        public ThumbnailViewHolder(View view) {
            super(view);

            imgThumbnail = (ImageView) view.findViewById(R.id.image_thumbnail);
            txtSelectionCount = (TextView) view.findViewById(R.id.text_selection_count);
            videoOverlay = view.findViewById(R.id.image_video_overlay);

            imgThumbnail.getLayoutParams().width = mThumbWidth;
            imgThumbnail.getLayoutParams().height = mThumbHeight;

            detector = new GestureDetector(view.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    int position = getAdapterPosition();
                    if (isValidPosition(position)) {
                        if (mIsMultiSelectEnabled) {
                            toggleSelection(ThumbnailViewHolder.this, position);
                        } else if (mListener != null) {
                            Uri uri = getItemAtPosition(position).uri;
                            mListener.onItemTapped(uri);
                        }
                    }
                    return true;
                }
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    int position = getAdapterPosition();
                    if (isValidPosition(position) && mListener != null) {
                        Uri uri = getItemAtPosition(position).uri;
                        mListener.onItemDoubleTapped(itemView, uri);
                    }
                    return true;
                }
                @Override
                public void onLongPress(MotionEvent e) {
                    int position = getAdapterPosition();
                    if (isValidPosition(position)) {
                        if (!mIsMultiSelectEnabled) {
                            setMultiSelectEnabled(true);
                        }
                        toggleSelection(ThumbnailViewHolder.this, position);
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

            try {
                int idIndex = cursor.getColumnIndexOrThrow(ID_COL);
                while (cursor.moveToNext()) {
                    PhotoChooserItem item = new PhotoChooserItem();
                    item._id = cursor.getLong(idIndex);
                    item.uri = Uri.withAppendedPath(baseUri, "" + item._id);
                    item.isVideo = isVideo;
                    tmpList.add(item);
                }
            } finally {
                SqlUtils.closeCursor(cursor);
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            mMediaList.clear();
            mMediaList.addAll(tmpList);
            notifyDataSetChanged();
            if (mListener != null) {
                mListener.onAdapterLoaded(isEmpty());
            }
        }
    }
}
