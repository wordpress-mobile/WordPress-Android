package org.wordpress.android.ui.themes;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Theme;

/**
 * Adapter for the {@link ThemeBrowserFragment}'s listview
 *
 */
public class ThemeBrowserAdapter extends CursorAdapter {
    private static final String THEME_IMAGE_PARAMETER = "?w=400";
    private final LayoutInflater mInflater;
    private final ThemeBrowserFragment.ThemeBrowserFragmentCallback mCallback;

    public ThemeBrowserAdapter(Context context, Cursor c, boolean autoRequery, ThemeBrowserFragment.ThemeBrowserFragmentCallback callback) {
        super(context, c, autoRequery);
        mInflater = LayoutInflater.from(context);
        mCallback = callback;
    }

    private static class ThemeViewHolder {
        private final CardView cardView;
        private final NetworkImageView imageView;
        private final TextView nameView;
        private final TextView activeView;
        private final TextView priceView;
        private final ImageButton imageButton;
        private final FrameLayout frameLayout;
        private final RelativeLayout detailsView;

        ThemeViewHolder(View view) {
            cardView = (CardView) view.findViewById(R.id.theme_grid_card);
            imageView = (NetworkImageView) view.findViewById(R.id.theme_grid_item_image);
            nameView = (TextView) view.findViewById(R.id.theme_grid_item_name);
            priceView = (TextView) view.findViewById(R.id.theme_grid_item_price);
            activeView = (TextView) view.findViewById(R.id.theme_grid_item_active);
            imageButton = (ImageButton) view.findViewById(R.id.theme_grid_item_image_button);
            frameLayout = (FrameLayout) view.findViewById(R.id.theme_grid_item_image_layout);
            detailsView = (RelativeLayout) view.findViewById(R.id.theme_grid_item_details);
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

        final String screenshotURL = cursor.getString(cursor.getColumnIndex(Theme.SCREENSHOT));
        final String name = cursor.getString(cursor.getColumnIndex(Theme.NAME));
        final String price = cursor.getString(cursor.getColumnIndex(Theme.PRICE));
        final String themeId = cursor.getString(cursor.getColumnIndex(Theme.ID));
        final boolean isCurrent = cursor.getInt(cursor.getColumnIndex(Theme.IS_CURRENT)) == 1;
        final boolean isPremium = !price.isEmpty();

        themeViewHolder.nameView.setText(name);
        themeViewHolder.priceView.setText(price);

        configureImageView(themeViewHolder, screenshotURL, themeId, isCurrent);
        configureImageButton(context, themeViewHolder, themeId, isPremium, isCurrent);
        configureCardView(context, themeViewHolder, isCurrent);
    }

    private void configureCardView(Context context, ThemeViewHolder themeViewHolder, boolean isCurrent) {
        if (isCurrent) {
            themeViewHolder.detailsView.setBackgroundColor(context.getResources().getColor(R.color.blue_wordpress));
            themeViewHolder.nameView.setTextColor(context.getResources().getColor(R.color.white));
            themeViewHolder.activeView.setVisibility(View.VISIBLE);
            themeViewHolder.cardView.setCardBackgroundColor(context.getResources().getColor(R.color.blue_wordpress));
        } else {
            themeViewHolder.detailsView.setBackgroundColor(context.getResources().getColor(R.color.cardview_light_background));
            themeViewHolder.nameView.setTextColor(context.getResources().getColor(R.color.black));
            themeViewHolder.activeView.setVisibility(View.GONE);
            themeViewHolder.cardView.setCardBackgroundColor(context.getResources().getColor(R.color.cardview_light_background));
        }
    }

    private void configureImageView(ThemeViewHolder themeViewHolder, String screenshotURL, final String themeId, final boolean isCurrent) {
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

        themeViewHolder.imageView.setImageUrl(screenshotURL + THEME_IMAGE_PARAMETER, WordPress.imageLoader);
        themeViewHolder.frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCurrent) {
                    mCallback.onTryAndCustomizeSelected(themeId);
                } else {
                    mCallback.onViewSelected(themeId);
                }
            }
        });
    }

    private void configureImageButton(Context context, ThemeViewHolder themeViewHolder, final String themeId, final boolean isPremium, boolean isCurrent) {
        final PopupMenu popupMenu = new PopupMenu(context, themeViewHolder.imageButton);
        popupMenu.getMenuInflater().inflate(R.menu.theme_more, popupMenu.getMenu());

        configureMenuForTheme(popupMenu.getMenu(), isCurrent);

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
                    case R.id.menu_try_and_customize:
                        mCallback.onTryAndCustomizeSelected(themeId);
                        break;
                    case R.id.menu_view:
                        mCallback.onViewSelected(themeId);
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

    private void configureMenuForTheme(Menu menu, boolean isCurrent) {
        MenuItem activate = menu.findItem(R.id.menu_activate);
        MenuItem customize = menu.findItem(R.id.menu_try_and_customize);
        MenuItem view = menu.findItem(R.id.menu_view);

        if (activate != null) {
            activate.setVisible(!isCurrent);
        }
        if (customize != null) {
            if (isCurrent) {
                customize.setTitle(R.string.customize);
            } else {
                customize.setTitle(R.string.theme_try_and_customize);
            }
        }
        if (view != null) {
            view.setVisible(!isCurrent);
        }
    }

    static class ScreenshotHolder {
        String requestURL;
    }
}
