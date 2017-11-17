package org.wordpress.android.ui.themes;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.annotation.NonNull;
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

import com.wellsql.generated.ThemeModelTable;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.HeaderGridView;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.ui.themes.ThemeBrowserFragment.ThemeBrowserFragmentCallback;

class ThemeBrowserAdapter extends CursorAdapter {
    private static final int HEADER_VIEW_TYPE = 1;
    private static final String HEADER_THEME_ID = "HEADER_THEME_ID";
    private static final String THEME_IMAGE_PARAMETER = "?w=";

    static Cursor createHeaderCursor(String headerText, int count) {
        MatrixCursor cursor = new MatrixCursor(new String[] {
                ThemeModelTable.ID, ThemeModelTable.THEME_ID, ThemeModelTable.NAME, "count"
        });
        cursor.addRow(new String[]{"0", HEADER_THEME_ID, headerText, String.valueOf(count)});
        return cursor;
    }

    static final String[] THEME_COLUMNS = new String[] {
            ThemeModelTable.ID, ThemeModelTable.THEME_ID, ThemeModelTable.NAME, ThemeModelTable.SCREENSHOT_URL,
            ThemeModelTable.CURRENCY, ThemeModelTable.PRICE, ThemeModelTable.ACTIVE
    };
    static String[] createThemeCursorRow(@NonNull ThemeModel theme) {
        return new String[] {
                String.valueOf(theme.getId()), theme.getThemeId(), theme.getName(), theme.getScreenshotUrl(),
                theme.getCurrency(), String.valueOf(theme.getPrice()), String.valueOf(theme.getActive())
        };
    }

    private final LayoutInflater mInflater;
    private final ThemeBrowserFragmentCallback mCallback;
    private int mViewWidth;

    ThemeBrowserAdapter(Context context, Cursor c, boolean autoRequery, ThemeBrowserFragmentCallback callback) {
        super(context, c, autoRequery);
        mInflater = LayoutInflater.from(context);
        mCallback = callback;
        mViewWidth = AppPrefs.getThemeImageSizeWidth();
    }

    private static class ThemeViewHolder {
        private final CardView cardView;
        private final WPNetworkImageView imageView;
        private final TextView nameView;
        private final TextView activeView;
        private final TextView priceView;
        private final ImageButton imageButton;
        private final FrameLayout frameLayout;
        private final RelativeLayout detailsView;

        ThemeViewHolder(View view) {
            cardView = (CardView) view.findViewById(R.id.theme_grid_card);
            imageView = (WPNetworkImageView) view.findViewById(R.id.theme_grid_item_image);
            nameView = (TextView) view.findViewById(R.id.theme_grid_item_name);
            priceView = (TextView) view.findViewById(R.id.theme_grid_item_price);
            activeView = (TextView) view.findViewById(R.id.theme_grid_item_active);
            imageButton = (ImageButton) view.findViewById(R.id.theme_grid_item_image_button);
            frameLayout = (FrameLayout) view.findViewById(R.id.theme_grid_item_image_layout);
            detailsView = (RelativeLayout) view.findViewById(R.id.theme_grid_item_details);
        }
    }

    private static class HeaderViewHolder {
        private final TextView headerText;
        private final TextView countText;

        HeaderViewHolder(View view) {
            headerText = (TextView) view.findViewById(R.id.section_header_text);
            countText = (TextView) view.findViewById(R.id.section_header_count);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        if (getItemViewType(cursor) == HEADER_VIEW_TYPE) {
            View view = mInflater.inflate(R.layout.theme_section_header, parent, false);
            HeaderViewHolder headerViewHolder = new HeaderViewHolder(view);
            view.setTag(headerViewHolder);
            return view;
        }

        View view = mInflater.inflate(R.layout.theme_grid_item, parent, false);
        configureThemeImageSize(parent);
        ThemeViewHolder themeViewHolder = new ThemeViewHolder(view);
        view.setTag(themeViewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (getItemViewType(cursor) == HEADER_VIEW_TYPE) {
            final HeaderViewHolder headerViewHolder = (HeaderViewHolder) view.getTag();
            final String headerText = cursor.getString(cursor.getColumnIndex(ThemeModelTable.NAME));
            final String countText = cursor.getString(cursor.getColumnIndex("count"));
            headerViewHolder.headerText.setText(headerText);
            headerViewHolder.countText.setText(countText);
            return;
        }

        String screenshotURL = cursor.getString(cursor.getColumnIndex(ThemeModelTable.SCREENSHOT_URL));
        final ThemeViewHolder themeViewHolder = (ThemeViewHolder) view.getTag();
        final String name = cursor.getString(cursor.getColumnIndex(ThemeModelTable.NAME));
        final String themeId = cursor.getString(cursor.getColumnIndex(ThemeModelTable.THEME_ID));
        final String currency = cursor.getString(cursor.getColumnIndex(ThemeModelTable.CURRENCY));
        final float price = cursor.getFloat(cursor.getColumnIndex(ThemeModelTable.PRICE));
        final boolean isPremium = price > 0.f;
        final boolean isCurrent =
                StringUtils.equals("true", cursor.getString(cursor.getColumnIndex(ThemeModelTable.ACTIVE)));

        themeViewHolder.nameView.setText(name);
        if (isPremium) {
            String priceText = currency + String.valueOf((int) price);
            themeViewHolder.priceView.setText(priceText);
            themeViewHolder.priceView.setVisibility(View.VISIBLE);
        } else {
            themeViewHolder.priceView.setVisibility(View.GONE);
        }

        // catch the case where a URL has no protocol
        if (!screenshotURL.startsWith(ThemeWebActivity.THEME_HTTP_PREFIX)) {
            // some APIs return a URL starting with // so the protocol can be supplied by the client
            // strip // before adding the protocol
            if (screenshotURL.startsWith("//")) {
                screenshotURL = screenshotURL.substring(2);
            }
            screenshotURL = ThemeWebActivity.THEME_HTTPS_PROTOCOL + screenshotURL;
        }

        configureImageView(themeViewHolder, screenshotURL, themeId, isCurrent);
        configureImageButton(context, themeViewHolder, themeId, isPremium, isCurrent);
        configureCardView(context, themeViewHolder, isCurrent);
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getItemViewType(cursor);
    }

    @Override
    public int getViewTypeCount() {
        // standard theme item view and a header view for Jetpack sites to section Uploaded/WP.com themes
        return 2;
    }

    @SuppressWarnings("deprecation")
    private void configureCardView(Context context, ThemeViewHolder themeViewHolder, boolean isCurrent) {
        Resources resources = context.getResources();
        if (isCurrent) {
            themeViewHolder.detailsView.setBackgroundColor(resources.getColor(R.color.blue_wordpress));
            themeViewHolder.nameView.setTextColor(resources.getColor(R.color.white));
            themeViewHolder.activeView.setVisibility(View.VISIBLE);
            themeViewHolder.cardView.setCardBackgroundColor(resources.getColor(R.color.blue_wordpress));
        } else {
            themeViewHolder.detailsView.setBackgroundColor(resources.getColor(
                    android.support.v7.cardview.R.color.cardview_light_background));
            themeViewHolder.nameView.setTextColor(resources.getColor(R.color.black));
            themeViewHolder.activeView.setVisibility(View.GONE);
            themeViewHolder.cardView.setCardBackgroundColor(resources.getColor(
                    android.support.v7.cardview.R.color.cardview_light_background));
        }
    }

    private void configureImageView(ThemeViewHolder themeViewHolder, String screenshotURL, final String themeId, final boolean isCurrent) {
        String requestURL = (String) themeViewHolder.imageView.getTag();
        if (requestURL == null) {
            requestURL = screenshotURL;
            themeViewHolder.imageView.setDefaultImageResId(R.drawable.theme_loading);
            themeViewHolder.imageView.setTag(requestURL);
        }

        if (!requestURL.equals(screenshotURL)) {
            requestURL = screenshotURL;
        }

        themeViewHolder.imageView.setImageUrl(requestURL + THEME_IMAGE_PARAMETER + mViewWidth, WPNetworkImageView.ImageType.PHOTO);
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
                int i = item.getItemId();
                if (i == R.id.menu_activate) {
                    if (isPremium) {
                        mCallback.onDetailsSelected(themeId);
                    } else {
                        mCallback.onActivateSelected(themeId);
                    }
                } else if (i == R.id.menu_try_and_customize) {
                    mCallback.onTryAndCustomizeSelected(themeId);
                } else if (i == R.id.menu_view) {
                    mCallback.onViewSelected(themeId);
                } else if (i == R.id.menu_details) {
                    mCallback.onDetailsSelected(themeId);
                } else {
                    mCallback.onSupportSelected(themeId);
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

    private void configureThemeImageSize(ViewGroup parent) {
        HeaderGridView gridView = (HeaderGridView) parent.findViewById(R.id.theme_listview);
        int numColumns = gridView.getNumColumns();
        int screenWidth = gridView.getWidth();
        int imageWidth = screenWidth / numColumns;
        if (imageWidth > mViewWidth) {
            mViewWidth = imageWidth;
            AppPrefs.setThemeImageSizeWidth(mViewWidth);
        }
    }

    private int getItemViewType(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndex(ThemeModelTable.THEME_ID));
        if (id.equals(HEADER_THEME_ID)) {
            return HEADER_VIEW_TYPE;
        }
        return 0;
    }
}
