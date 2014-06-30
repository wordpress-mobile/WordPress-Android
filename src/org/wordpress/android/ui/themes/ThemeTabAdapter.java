package org.wordpress.android.ui.themes;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.DisplayUtils;

/**
 * Adapter for the {@link ThemeTabFragment}'s gridview
 *
 */
public class ThemeTabAdapter extends CursorAdapter {
    private final LayoutInflater mInflater;
    private final int mColumnWidth;
    private final int mColumnHeight;
    private final Drawable mIconPremium;
    private final Drawable mIconCurrent;
    private final int m32DpToPx;

    public ThemeTabAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        mInflater = LayoutInflater.from(context);
        mColumnWidth = getColumnWidth(context);
        mColumnHeight = (int) (0.75f * mColumnWidth);
        mIconPremium = context.getResources().getDrawable(R.drawable.theme_icon_tag_premium);
        mIconCurrent = context.getResources().getDrawable(R.drawable.theme_icon_tag_current);
        m32DpToPx = DisplayUtils.dpToPx(context, 32);
    }

    private static class ThemeViewHolder {
        private final NetworkImageView imageView;
        private final TextView nameView;
        private final ImageView themeAttr;

        ThemeViewHolder(View view) {
            imageView = (NetworkImageView) view.findViewById(R.id.theme_grid_item_image);
            nameView = (TextView) view.findViewById(R.id.theme_grid_item_name);
            themeAttr = (ImageView) view.findViewById(R.id.theme_grid_attributes);
        }
    }
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.theme_grid_item, parent, false);

        ThemeViewHolder themeViewHolder = new ThemeViewHolder(view);
        view.setTag(themeViewHolder);

        // size the imageView to fit the column - image will be requested at this same width
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) themeViewHolder.imageView.getLayoutParams();
        params.width = mColumnWidth;
        params.height = mColumnHeight;

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ThemeViewHolder themeViewHolder = (ThemeViewHolder) view.getTag();

        final String screenshotURL =  cursor.getString(cursor.getColumnIndex("screenshotURL"));
        final String name = cursor.getString(cursor.getColumnIndex("name"));
        final int isPremiumTheme = cursor.getInt(cursor.getColumnIndex("isPremium"));
        final int isCurrentTheme = cursor.getInt(cursor.getColumnIndex("isCurrent"));

        themeViewHolder.nameView.setText(name);

        if (isCurrentTheme != 0) {
            themeViewHolder.themeAttr.setVisibility(View.VISIBLE);
            themeViewHolder.themeAttr.setImageDrawable(mIconCurrent);
        } else if (isPremiumTheme != 0) {
            themeViewHolder.themeAttr.setVisibility(View.VISIBLE);
            themeViewHolder.themeAttr.setImageDrawable(mIconPremium);
        } else {
            themeViewHolder.themeAttr.setVisibility(View.GONE);
        }

        ScreenshotHolder urlHolder = (ScreenshotHolder) themeViewHolder.imageView.getTag();
        if (urlHolder == null) {
            urlHolder = new ScreenshotHolder();
            urlHolder.requestURL = screenshotURL;
            themeViewHolder.imageView.setTag(urlHolder);
        }

        if (!urlHolder.requestURL.equals(screenshotURL)) {
            themeViewHolder.imageView.setImageBitmap(null);
            urlHolder.requestURL = screenshotURL;
        }

        themeViewHolder.imageView.setImageUrl(screenshotURL + "?w=" + mColumnWidth, WordPress.imageLoader);
        view.setLayoutParams(new GridView.LayoutParams(mColumnWidth, mColumnHeight + m32DpToPx));
    }

    // The theme previews are 600x450 px, resulting in a ratio of 0.75
    // We'll try to max the width, while keeping the padding ratio correct.
    // Then we'll determine the height based on the width and the 0.75 ratio
    private static int getColumnWidth(Context context) {
        // Padding is 4 dp between the grid columns and on the outside
        int columnCount = context.getResources().getInteger(R.integer.themes_grid_num_columns);
        int dp4 = DisplayUtils.dpToPx(context, 4);
        int padding = (columnCount + 1) * dp4;

        // the max width of the themes is either:
        // = width of entire screen (phone and tablet portrait)
        // = width of entire screen - menu drawer width (tablet landscape)
        int maxWidth = context.getResources().getDisplayMetrics().widthPixels;
        if (DisplayUtils.isXLarge(context) && DisplayUtils.isLandscape(context))
            maxWidth -= context.getResources().getDimensionPixelSize(R.dimen.menu_drawer_width);

        return (maxWidth - padding) / columnCount;
    }

    static class ScreenshotHolder {
        String requestURL;
    }
}