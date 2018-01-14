package org.wordpress.android.ui.themes;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.themes.ThemeBrowserFragment.ThemeBrowserFragmentCallback;
import org.wordpress.android.widgets.HeaderGridView;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.List;

class ThemeBrowserAdapter extends BaseAdapter implements Filterable {
    private static final String THEME_IMAGE_PARAMETER = "?w=";

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final ThemeBrowserFragmentCallback mCallback;

    private int mViewWidth;
    private String mQuery;
    private final boolean mIsWpCom;

    private final List<ThemeModel> mAllThemes = new ArrayList<>();
    private final List<ThemeModel> mFilteredThemes = new ArrayList<>();
    private final SparseArray<ThemeSectionHeader> mHeaders = new SparseArray<>();

    class ThemeSectionHeader {
        final String text;
        final int count;

        ThemeSectionHeader(@NonNull String text, int count) {
            this.text = text;
            this.count = count;
        }
    }

    ThemeBrowserAdapter(Context context, boolean isWpCom, ThemeBrowserFragmentCallback callback) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mCallback = callback;
        mViewWidth = AppPrefs.getThemeImageSizeWidth();
        mIsWpCom = isWpCom;
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

        private final ViewGroup headerView;
        private final TextView headerText;
        private final TextView headerCount;

        ThemeViewHolder(View view) {
            cardView = view.findViewById(R.id.theme_grid_card);
            imageView = view.findViewById(R.id.theme_grid_item_image);
            nameView = view.findViewById(R.id.theme_grid_item_name);
            priceView = view.findViewById(R.id.theme_grid_item_price);
            activeView = view.findViewById(R.id.theme_grid_item_active);
            imageButton = view.findViewById(R.id.theme_grid_item_image_button);
            frameLayout = view.findViewById(R.id.theme_grid_item_image_layout);
            detailsView = view.findViewById(R.id.theme_grid_item_details);

            headerView = view.findViewById(R.id.section_header);
            headerText = headerView.findViewById(R.id.section_header_text);
            headerCount = headerView.findViewById(R.id.section_header_count);
        }
    }

    @Override
    public int getCount() {
        return mFilteredThemes.size();
    }

    @Override
    public Object getItem(int position) {
        return mFilteredThemes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    void setThemeList(@NonNull List<ThemeModel> themes) {
        mAllThemes.clear();
        mAllThemes.addAll(themes);

        mFilteredThemes.clear();
        mFilteredThemes.addAll(themes);

        if (!TextUtils.isEmpty(mQuery)) {
            getFilter().filter(mQuery);
        } else {
            updateHeaders();
            notifyDataSetChanged();
        }
    }

    /*
     * jetpack sites have headers above the uploaded themes and wp.com themes
     */
    private void updateHeaders() {
        if (mIsWpCom) return;

        mHeaders.clear();
        if (mFilteredThemes.size() == 0) return;

        // first count the two types of themes
        int numUploadedThemes = 0;
        int numWpComThemes = 0;
        for (ThemeModel theme: mFilteredThemes) {
            if (theme.isWpComTheme()) {
                numWpComThemes++;
            } else {
                numUploadedThemes++;
            }
        }

        // then create the headers
        for (int i = 0; i < mFilteredThemes.size(); i++) {
            ThemeModel theme = mFilteredThemes.get(i);
            if (i == 0 && !theme.isWpComTheme()) {
                // add uploaded themes header if this is the first theme and it's not wp.com
                String text = mContext.getString(R.string.uploaded_themes_header);
                mHeaders.put(i, new ThemeSectionHeader(text, numUploadedThemes));
            } else if (theme.isWpComTheme()) {
                // add wp.com themes header if this is the first wp.com theme
                String text = mContext.getString(R.string.wpcom_themes_header);
                mHeaders.put(i, new ThemeSectionHeader(text, numWpComThemes));
                break;
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ThemeViewHolder holder;
        if (convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.theme_grid_item, parent, false);
            holder = new ThemeViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ThemeViewHolder) convertView.getTag();
        }

        configureThemeImageSize(parent);
        ThemeModel theme = mFilteredThemes.get(position);

        String screenshotURL = theme.getScreenshotUrl();
        String themeId = theme.getThemeId();
        boolean isPremium = !theme.isFree();
        boolean isCurrent = theme.getActive();

        holder.nameView.setText(theme.getName());
        if (isPremium) {
            holder.priceView.setText(theme.getPriceText());
            holder.priceView.setVisibility(View.VISIBLE);
        } else {
            holder.priceView.setVisibility(View.GONE);
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

        configureImageView(holder, screenshotURL, themeId, isCurrent);
        configureImageButton(holder, themeId, isPremium, isCurrent);
        configureCardView(holder, isCurrent);

        // show a section header if one exists at this position
        ThemeSectionHeader header = mHeaders.get(position);
        if (header != null) {
            holder.headerView.setVisibility(View.VISIBLE);
            holder.headerText.setText(header.text);
            holder.headerCount.setText(String.valueOf(header.count));
        } else {
            holder.headerView.setVisibility(View.GONE);
        }

        return convertView;
    }

    @SuppressWarnings("deprecation")
    private void configureCardView(ThemeViewHolder themeViewHolder, boolean isCurrent) {
        Resources resources = mContext.getResources();
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

    private void configureImageButton(ThemeViewHolder themeViewHolder, final String themeId, final boolean isPremium, boolean isCurrent) {
        final PopupMenu popupMenu = new PopupMenu(mContext, themeViewHolder.imageButton);
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
        HeaderGridView gridView = parent.findViewById(R.id.theme_listview);
        int numColumns = gridView.getNumColumns();
        int screenWidth = gridView.getWidth();
        int imageWidth = screenWidth / numColumns;
        if (imageWidth > mViewWidth) {
            mViewWidth = imageWidth;
            AppPrefs.setThemeImageSizeWidth(mViewWidth);
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mFilteredThemes.clear();
                mFilteredThemes.addAll((List<ThemeModel>) results.values);
                updateHeaders();
                ThemeBrowserAdapter.this.notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<ThemeModel> filtered = new ArrayList<>();
                if (TextUtils.isEmpty(constraint)) {
                    mQuery = null;
                    filtered.addAll(mAllThemes);
                } else {
                    mQuery = constraint.toString();
                    String lcConstraint = constraint.toString().toLowerCase();
                    for (ThemeModel theme : mAllThemes) {
                        if (theme.getName().toLowerCase().contains(lcConstraint)) {
                            filtered.add(theme);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filtered;

                return results;
            }
        };
    }
}
