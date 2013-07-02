package org.wordpress.android.ui.media;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public class MediaItemListAdapter extends CursorAdapter {

    private static enum Type {
        IMAGE, // for image media that come with thumbnails (e.g. jpg)
        DEFAULT // for other media types that have a generic thumbnail (e.g. doc)
    }
    
    public MediaItemListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public void bindView(final View view, Context context, Cursor cursor) {
        TextView title = (TextView) view.findViewById(R.id.media_listitem_title);
        title.setText(cursor.getString(cursor.getColumnIndex("title")));
        
        String thumbnailURL = cursor.getString(cursor.getColumnIndex("thumbnailURL"));

        int viewType = getItemViewType(cursor);

        if (viewType == Type.IMAGE.ordinal()) {
            NetworkImageView thumbnail = (NetworkImageView) view.findViewById(R.id.media_listitem_thumbnail);
            thumbnail.setDefaultImageResId(R.drawable.placeholder);
            thumbnail.setImageUrl(thumbnailURL, WordPress.imageLoader);
        } else {
            
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup root) {
        LayoutInflater inflater = LayoutInflater.from(context);
        
        int viewType = getItemViewType(cursor);
        View view = null;

        if (viewType == Type.IMAGE.ordinal())
            view = inflater.inflate(R.layout.media_listitem_image, root, false);
        else
            view = inflater.inflate(R.layout.media_listitem_default, root, false);
        
        return view;
    }
   
    
    @Override
    public int getViewTypeCount() {
        return Type.values().length; 
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getItemViewType(cursor);
    }

    private int getItemViewType(Cursor cursor) {
        String fileURL = cursor.getString(cursor.getColumnIndex("fileURL"));
        if (MediaUtils.isValidImage(fileURL)) {
            return Type.IMAGE.ordinal();
        } else {
            return Type.DEFAULT.ordinal();
        }
    }

}