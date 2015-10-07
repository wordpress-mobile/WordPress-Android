package org.wordpress.android.ui.themes;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

/**
 * Adapter for the {@link ThemeBrowserFragment}'s listview
 *
 */
public class ThemeBrowserAdapter extends CursorAdapter {
    private final LayoutInflater mInflater;
    private final ThemeBrowserFragment.ThemeBrowserFragmentCallback mCallback;

    public ThemeBrowserAdapter(Context context, Cursor c, boolean autoRequery, ThemeBrowserFragment.ThemeBrowserFragmentCallback callback) {
        super(context, c, autoRequery);
        mInflater = LayoutInflater.from(context);
        mCallback = callback;
    }

    private static class ThemeViewHolder {
        private final NetworkImageView imageView;
        private final TextView nameView;
        private final TextView priceView;
        private final ImageButton imageButton;
        private final FrameLayout frameLayout;

        ThemeViewHolder(View view) {
            imageView = (NetworkImageView) view.findViewById(R.id.theme_grid_item_image);
            nameView = (TextView) view.findViewById(R.id.theme_grid_item_name);
            priceView = (TextView) view.findViewById(R.id.theme_grid_item_price);
            imageButton = (ImageButton) view.findViewById(R.id.theme_grid_item_image_button);
            frameLayout = (FrameLayout) view.findViewById(R.id.theme_grid_item_image_layout);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.theme_grid_item, parent, false);

        ThemeViewHolder themeViewHolder = new ThemeViewHolder(view);
        view.setTag(themeViewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ThemeViewHolder themeViewHolder = (ThemeViewHolder) view.getTag();

        final String screenshotURL = cursor.getString(cursor.getColumnIndex("screenshot"));
        final String name = cursor.getString(cursor.getColumnIndex("name"));
        final String price = cursor.getString(cursor.getColumnIndex("price"));
        final String themeId = cursor.getString(cursor.getColumnIndex("id"));
        final boolean isPremium = !price.isEmpty();

        themeViewHolder.nameView.setText(name);
        themeViewHolder.priceView.setText(price);

        configureImageView(themeViewHolder, screenshotURL, themeId);
        configureImageButton(context, themeViewHolder, themeId, isPremium);
    }

    private void configureImageView(ThemeViewHolder themeViewHolder, String screenshotURL, final String themeId) {
        ScreenshotHolder urlHolder = (ScreenshotHolder) themeViewHolder.imageView.getTag();
        if (urlHolder == null) {
            urlHolder = new ScreenshotHolder();
            urlHolder.requestURL = screenshotURL;
            themeViewHolder.imageView.setDefaultImageResId(R.drawable.theme_loading);
            themeViewHolder.imageView.setTag(urlHolder);
        }

        if (!urlHolder.requestURL.equals(screenshotURL)) {
            urlHolder.requestURL = screenshotURL;
        }

        themeViewHolder.imageView.setImageUrl(screenshotURL + "?w=" + 500, WordPress.imageLoader);
        themeViewHolder.frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onPreviewSelected(themeId);
            }
        });
    }

    private void configureImageButton(Context context, ThemeViewHolder themeViewHolder, final String themeId, final boolean isPremium) {
        final PopupMenu popupMenu = new PopupMenu(context, themeViewHolder.imageButton);
        popupMenu.getMenuInflater().inflate(R.menu.theme_more, popupMenu.getMenu());
        
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_activate:
                        if (isPremium) {
                            mCallback.onDetailsSelected(themeId);
                        } else {
                            mCallback.onActivateSelected(themeId);
                        }
                        break;
                    case R.id.menu_preview:
                        mCallback.onPreviewSelected(themeId);
                        break;
                    case R.id.menu_demo:
                        mCallback.onDemoSelected(themeId);
                        break;
                    case R.id.menu_details:
                        mCallback.onDetailsSelected(themeId);
                        break;
                    case R.id.menu_support:
                    default:
                        mCallback.onSupportSelected(themeId);
                        break;
                }

                return true;
            }
        });
        themeViewHolder.imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenu.show();
            }
        });
    }

    static class ScreenshotHolder {
        String requestURL;
    }
}
