package org.wordpress.android.ui.themes;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.GridView;

import com.android.volley.toolbox.NetworkImageView;

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
        
        final String screenshotURL =  cursor.getString(cursor.getColumnIndex("screenshotURL"));
        
        final NetworkImageView imageView = (NetworkImageView) view.findViewById(R.id.theme_grid_item_image);

        ViewHolder holder = (ViewHolder) imageView.getTag();
        if (holder == null) {
            holder = new ViewHolder();
            holder.requestURL = screenshotURL;
            imageView.setTag(holder);
        }
        
        if (!holder.requestURL.equals(screenshotURL)) {
            imageView.setImageBitmap(null);
            holder.requestURL = screenshotURL;
            
        }

        // load image in this way to get it sized properly for the imageview
        FrameLayout.LayoutParams params = (LayoutParams) imageView.getLayoutParams();
        params.width = getGridWidth(context);
        params.height = getGridHeight(context);
        imageView.setImageUrl(screenshotURL, WordPress.imageLoader);
        view.setLayoutParams(new GridView.LayoutParams(params.width, params.height));
    }

    // The theme previews are 600x450 px, resulting in a ratio of 0.75
    // We'll try to max the width, while keeping the padding ratio correct.
    // Then we'll determine the height based on the width and the 0.75 ratio
    
    private int getGridWidth(Context context) {
        // Padding is 12 dp between the grid columns and on the outside
        int columnCount = getColumnCount(context);
        int dp12 = (int) dpToPx(context, 12);
        int padding = (columnCount + 1) * dp12;
        
        // the max width of the themes is either:
        // = width of entire screen (phone and tablet portrait)
        // = width of entire screen - menu drawer width (tablet landscape)
        int maxWidth = context.getResources().getDisplayMetrics().widthPixels;
        if (isXLarge(context) && isLandscape(context))
            maxWidth -= context.getResources().getDimensionPixelSize(R.dimen.menu_drawer_width);
        
        return (int) (maxWidth - padding) / columnCount;
    }

    private int getGridHeight(Context context) {
        return (int) (0.75f * getGridWidth(context));
    }
    
    private int getColumnCount(Context context) {
        return context.getResources().getInteger(R.integer.themes_grid_num_columns);
    }
    
    static class ViewHolder {
        String requestURL;
    }

    private float dpToPx(Context context, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
   }
    
    // logic below based on login in WPActionBarActivity.java
    private boolean isXLarge(Context context) {
        if ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE)
            return true;
        return false;
    }
    
    private boolean isLandscape(Context context) {
        return (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }
    
}