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
import org.wordpress.android.util.DisplayUtils;

import java.util.ArrayList;

public class PhotoChooserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private final int mImageSz;
    private ArrayList<Uri> mUriList;

    public PhotoChooserAdapter(Context context) {
        super();

        mContext = context;
        int displayWidth = DisplayUtils.getDisplayPixelWidth(mContext);
        mImageSz = displayWidth / 4;
    }

    private static final String ID_COL = MediaStore.Images.Thumbnails._ID;

    public void loadGallery() {
        String[] projection = { ID_COL };

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
        Cursor[] cursorArray =  { external, internal };
        MergeCursor cursor = new MergeCursor(cursorArray);

        // create array of image Uris
        mUriList = new ArrayList<>();
        int index = cursor.getColumnIndexOrThrow(ID_COL);
        while (cursor.moveToNext()) {
            int imageID = cursor.getInt(index);
            Uri imageUri = Uri.withAppendedPath(
                    MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, "" + imageID);
            mUriList.add(imageUri);
        }
    }

    public boolean isEmpty() {
        return (getItemCount() == 0);
    }

    @Override
    public int getItemCount() {
        return mUriList != null ? mUriList.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_chooser_image, parent, false);
        return new PhotoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ImageView imageView = ((PhotoViewHolder) holder).imageView;
        imageView.setImageURI(mUriList.get(position));
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        public PhotoViewHolder(View view) {
            super(view);
            imageView = (ImageView) view.findViewById(R.id.image_photo);
            imageView.getLayoutParams().width = mImageSz;
            imageView.getLayoutParams().height = mImageSz;
        }
    }
}
