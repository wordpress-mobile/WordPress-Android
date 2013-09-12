package org.wordpress.android.ui.themes;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.Utils;

/**
 * Adapter for the {@link ThemeTabFragment}'s gridview
 *
 */
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
        final String name = cursor.getString(cursor.getColumnIndex("name"));
        
        final NetworkImageView imageView = (NetworkImageView) view.findViewById(R.id.theme_grid_item_image);
        final TextView nameView = (TextView) view.findViewById(R.id.theme_grid_item_name);
        nameView.setText(name);

        ImageView themeAttr = (ImageView) view.findViewById(R.id.theme_grid_attributes);
        
        final int isPremiumTheme = cursor.getInt(cursor.getColumnIndex("isPremium"));
        final int isCurrentTheme = cursor.getInt(cursor.getColumnIndex("isCurrent"));
        
        if (isCurrentTheme != 0) {
            themeAttr.setVisibility(View.VISIBLE);
            themeAttr.setImageResource(R.drawable.theme_icon_tag_current);
        } else if (isPremiumTheme != 0) { 
            themeAttr.setVisibility(View.VISIBLE);
            themeAttr.setImageResource(R.drawable.theme_icon_tag_premium);
        } else {
            themeAttr.setVisibility(View.GONE);
        }

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
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
        params.width = getGridWidth(context);
        params.height = getGridHeight(context);
        
        imageView.setImageUrl(screenshotURL + "?w=" + params.width, WordPress.imageLoader);
        view.setLayoutParams(new GridView.LayoutParams(params.width, params.height + ((int) Utils.dpToPx(32))));
    }

    // The theme previews are 600x450 px, resulting in a ratio of 0.75
    // We'll try to max the width, while keeping the padding ratio correct.
    // Then we'll determine the height based on the width and the 0.75 ratio
    
    private int getGridWidth(Context context) {
        // Padding is 4 dp between the grid columns and on the outside
        int columnCount = getColumnCount(context);
        int dp4 = (int) Utils.dpToPx(4);
        int padding = (columnCount + 1) * dp4;
        
        // the max width of the themes is either:
        // = width of entire screen (phone and tablet portrait)
        // = width of entire screen - menu drawer width (tablet landscape)
        int maxWidth = context.getResources().getDisplayMetrics().widthPixels;
        if (Utils.isXLarge(context) && Utils.isLandscape(context))
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
    
}