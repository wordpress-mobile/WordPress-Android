package org.wordpress.android.ui.themes;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
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
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.elevation.ElevationOverlayProvider;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.ui.plans.PlansConstants;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.themes.ThemeBrowserFragment.ThemeBrowserFragmentCallback;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;
import org.wordpress.android.widgets.HeaderGridView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ThemeBrowserAdapter extends BaseAdapter implements Filterable {
    private static final String THEME_IMAGE_PARAMETER = "?w=";

    private final Context mContext;
    private final long mSitePlanId;
    private final LayoutInflater mInflater;
    private final ThemeBrowserFragmentCallback mCallback;
    private final ImageManager mImageManager;

    private int mViewWidth;
    private String mQuery;

    private int mElevatedSurfaceColor;

    private final List<ThemeModel> mAllThemes = new ArrayList<>();
    private final List<ThemeModel> mFilteredThemes = new ArrayList<>();

    ThemeBrowserAdapter(Context context, long sitePlanId, ThemeBrowserFragmentCallback callback,
                        ImageManager imageManager) {
        mContext = context;
        mSitePlanId = sitePlanId;
        mInflater = LayoutInflater.from(context);
        mCallback = callback;
        mViewWidth = AppPrefs.getThemeImageSizeWidth();
        mImageManager = imageManager;

        ElevationOverlayProvider elevationOverlayProvider = new ElevationOverlayProvider(mContext);
        float cardElevation = mContext.getResources().getDimension(R.dimen.card_elevation);
        mElevatedSurfaceColor =
                elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(cardElevation);
    }

    private static class ThemeViewHolder {
        private final CardView mCardView;
        private final ImageView mImageView;
        private final TextView mNameView;
        private final TextView mActiveView;
        private final TextView mPriceView;
        private final ImageButton mImageButton;
        private final FrameLayout mFrameLayout;
        private final RelativeLayout mDetailsView;

        ThemeViewHolder(View view) {
            mCardView = view.findViewById(R.id.theme_grid_card);
            mImageView = view.findViewById(R.id.theme_grid_item_image);
            mNameView = view.findViewById(R.id.theme_grid_item_name);
            mPriceView = view.findViewById(R.id.theme_grid_item_price);
            mActiveView = view.findViewById(R.id.theme_grid_item_active);
            mImageButton = view.findViewById(R.id.theme_grid_item_image_button);
            mFrameLayout = view.findViewById(R.id.theme_grid_item_image_layout);
            mDetailsView = view.findViewById(R.id.theme_grid_item_details);
        }
    }

    @Override
    public int getCount() {
        return mFilteredThemes.size();
    }

    public int getUnfilteredCount() {
        return mAllThemes.size();
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
            notifyDataSetChanged();
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

        holder.mNameView.setText(theme.getName());
        if (isPremium) {
            holder.mPriceView.setText(theme.getPriceText());
            holder.mPriceView.setVisibility(View.VISIBLE);
        } else {
            holder.mPriceView.setVisibility(View.GONE);
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
        return convertView;
    }

    @SuppressWarnings("deprecation")
    private void configureCardView(ThemeViewHolder themeViewHolder, boolean isCurrent) {
        if (isCurrent) {
            ColorStateList color =
                    ContextExtensionsKt.getColorStateListFromAttribute(mContext, R.attr.colorOnPrimarySurface);
            themeViewHolder.mDetailsView
                    .setBackgroundColor(
                            ContextExtensionsKt.getColorFromAttribute(mContext, R.attr.colorPrimary));
            themeViewHolder.mNameView.setTextColor(color);
            themeViewHolder.mActiveView.setVisibility(View.VISIBLE);
            themeViewHolder.mCardView
                    .setCardBackgroundColor(
                            ContextExtensionsKt.getColorFromAttribute(mContext, R.attr.colorPrimary));
            ImageViewCompat.setImageTintList(themeViewHolder.mImageButton, color);
        } else {
            ColorStateList color = ContextExtensionsKt.getColorStateListFromAttribute(mContext, R.attr.colorOnSurface);
            themeViewHolder.mDetailsView
                    .setBackgroundColor(mElevatedSurfaceColor);
            themeViewHolder.mNameView.setTextColor(color);
            themeViewHolder.mActiveView.setVisibility(View.GONE);
            themeViewHolder.mCardView
                    .setCardBackgroundColor(mElevatedSurfaceColor);
            ImageViewCompat.setImageTintList(themeViewHolder.mImageButton, color);
        }
    }

    private void configureImageView(ThemeViewHolder themeViewHolder, String screenshotURL, final String themeId,
                                    final boolean isCurrent) {
        mImageManager
                .load(themeViewHolder.mImageView, ImageType.THEME, screenshotURL + THEME_IMAGE_PARAMETER + mViewWidth,
                        ScaleType.FIT_CENTER);

        themeViewHolder.mCardView.setOnClickListener(new View.OnClickListener() {
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

    private void configureImageButton(ThemeViewHolder themeViewHolder, final String themeId, final boolean isPremium,
                                      boolean isCurrent) {
        final PopupMenu popupMenu = new PopupMenu(mContext, themeViewHolder.mImageButton);
        popupMenu.getMenuInflater().inflate(R.menu.theme_more, popupMenu.getMenu());

        configureMenuForTheme(popupMenu.getMenu(), isCurrent);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int i = item.getItemId();
                if (i == R.id.menu_activate) {
                    if (canActivateThemeDirectly(isPremium, mSitePlanId)) {
                        mCallback.onActivateSelected(themeId);
                    } else {
                        // forward the user online to complete the activation
                        mCallback.onDetailsSelected(themeId);
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
        themeViewHolder.mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenu.show();
            }
        });
    }

    private boolean canActivateThemeDirectly(final boolean isPremiumTheme, final long sitePlanId) {
        if (!isPremiumTheme) {
            // It's a free theme so, can always activate directly
            return true;
        }

        if (sitePlanId == PlansConstants.PREMIUM_PLAN_ID || mSitePlanId == PlansConstants.BUSINESS_PLAN_ID) {
            // Can activate any theme on a Premium and Business site plan
            return true;
        }

        // Theme cannot be activated directly and needs to be purchased
        return false;
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
                    // Locale.ROOT is used on user input for convenience as all the theme names are in english
                    String lcConstraint = constraint.toString().toLowerCase(Locale.ROOT);
                    for (ThemeModel theme : mAllThemes) {
                        if (theme.getName().toLowerCase(Locale.ROOT).contains(lcConstraint)) {
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
