package org.wordpress.android.ui.posts;

import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.wordpress.android.R;

public class PhotoChooserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private Cursor mCursor;

    public PhotoChooserAdapter(Context context) {
        super();
        mContext = context;
        setHasStableIds(true);
    }

    private static final String ID_COL = MediaStore.Images.Thumbnails._ID;

    public void loadGallery() {
        String[] projection = {ID_COL};

        // create cursor containing external (SDCARD) images
        Cursor external = mContext.getContentResolver().query(
                MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                projection, // Which columns to return
                null,       // Return all rows
                null,
                ID_COL);
        // create cursor for internal images
        Cursor internal = mContext.getContentResolver().query(
                MediaStore.Images.Thumbnails.INTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                ID_COL);

        // merge the two cursors
        Cursor[] cursorArray =  { external, internal};
        mCursor = new MergeCursor(cursorArray);
    }

    public boolean isEmpty() {
        return (getItemCount() == 0);
    }

    @Override
    public int getItemCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        int index = mCursor.getColumnIndexOrThrow(ID_COL);
        return mCursor.getInt(index);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_chooser_image, parent, false);
        return new PhotoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        mCursor.moveToPosition(position);

        int index = mCursor.getColumnIndexOrThrow(ID_COL);
        int imageID = mCursor.getInt(index);

        ImageView imageView = ((PhotoViewHolder) holder).imageView;
        imageView.setImageURI(Uri.withAppendedPath(
                MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, "" + imageID));
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        public PhotoViewHolder(View view) {
            super(view);
            imageView = (ImageView) view.findViewById(R.id.image_photo);
        }
    }
}
