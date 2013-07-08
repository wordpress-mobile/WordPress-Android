package org.wordpress.android.ui.themes;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Theme;

public class ThemeTabAdapter extends BaseAdapter {

    private List<Theme> mThemes;
    private Context mContext;

    public ThemeTabAdapter(Context context, List<Theme> themes) {
        mContext = context;
        mThemes = themes;
    }

    public void setThemes(List<Theme> themes) {
        mThemes = themes;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mThemes.size();
    }

    @Override
    public Object getItem(int position) {
        return mThemes.get(position);
    }

    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        
//        skip view recycling for now, as we expect to replace this with a cursor adapter later.
        
//        if (convertView == null) {
//            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
//            convertView = inflater.inflate(R.layout.theme_grid_item, parent, false); 
//        } else {
//            
//        }
        
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        convertView = inflater.inflate(R.layout.theme_grid_item, parent, false);
        
        NetworkImageView imageView = (NetworkImageView) convertView.findViewById(R.id.theme_grid_item_image);
        imageView.setImageUrl(mThemes.get(position).getScreenshotURL(), WordPress.imageLoader);
        
        updateGridWidth(mContext, convertView);   
        
        return convertView;
    }
    

    private void updateGridWidth(Context context, View view) {
        // make it so that total padding = 1/12 of screen width
        // since there are two columns on the grid, each column will get
        // 11/24 of the remaining space available
        
        // (the padding is based on the mocks)

        // phone-size - full screen - use entire screen
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        
        // TODO: tablet-size
        
        // The theme previews are 600x450 px, resulting in a ratio of 0.75
        // We'll try to max the width, while keeping the padding ratio correct.
        // Then we'll determine the height based on the width and the 0.75 ratio
        
        float width = (int) (screenWidth * 11.0f / 24.0f);
        float height = 0.75f * width;
        view.setLayoutParams(new GridView.LayoutParams((int) width, (int) height));
    }

}