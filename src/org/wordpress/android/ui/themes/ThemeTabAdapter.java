package org.wordpress.android.ui.themes;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public class ThemeTabAdapter extends CursorAdapter {

    public ThemeTabAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.theme_grid_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        
        String screenshotURL =  cursor.getString(cursor.getColumnIndex("screenshotURL"));
        
        final ImageView imageView = (ImageView) view.findViewById(R.id.theme_grid_item_image);
        imageView.setImageBitmap(null);
        
        // load image in this way to get it sized properly for the imageview
        WordPress.imageLoader.get(screenshotURL, new ImageListener() {
            
            @Override
            public void onErrorResponse(VolleyError error) { }
            
            @Override
            public void onResponse(ImageContainer response, boolean isImmediate) {
                if (response != null && response.getBitmap() != null)
                    imageView.setImageBitmap(response.getBitmap());
                else
                    imageView.setImageBitmap(null);
                
            }
        }, getGridWidth(context), getGridHeight(context));
        
        updateGridWidth(mContext, view);   
    }

    private void updateGridWidth(Context context, View view) {
        // make it so that total padding = 1/12 of screen width
        // since there are two columns on the grid, each column will get
        // 11/24 of the remaining space available
        
        // (the padding is based on the mocks)
        
        view.setLayoutParams(new GridView.LayoutParams(getGridWidth(context), getGridHeight(context)));
    }

    // The theme previews are 600x450 px, resulting in a ratio of 0.75
    // We'll try to max the width, while keeping the padding ratio correct.
    // Then we'll determine the height based on the width and the 0.75 ratio
    
    private int getGridWidth(Context context) {
        // for phone-size, use entire screen
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        return (int) (screenWidth * 11.0f / 24.0f);
    }
    
    private int getGridHeight(Context context) {
        return (int) (0.75f * getGridWidth(context));
    }
}