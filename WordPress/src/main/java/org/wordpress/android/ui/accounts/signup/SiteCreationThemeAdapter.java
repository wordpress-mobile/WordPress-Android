package org.wordpress.android.ui.accounts.signup;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;

public class SiteCreationThemeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private boolean mIsLoading;
    private List<ThemeModel> mThemes;
    private @StringRes int mErrorMessage;
    private SiteCreationListener mSiteCreationListener;

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public final View progressContainer;
        public final View progress;
        public final TextView label;

        HeaderViewHolder(View itemView) {
            super(itemView);
            this.progressContainer = itemView.findViewById(R.id.progress_container);
            this.progress = itemView.findViewById(R.id.progress_bar);
            this.label = (TextView) itemView.findViewById(R.id.progress_label);
        }
    }

    public static class ThemeViewHolder extends RecyclerView.ViewHolder {
        private final WPNetworkImageView imageView;
        private final TextView nameView;

        ThemeViewHolder(View view) {
            super(view);
            imageView = (WPNetworkImageView) view.findViewById(R.id.theme_grid_item_image);
            nameView = (TextView) view.findViewById(R.id.theme_grid_item_name);
        }
    }

    public SiteCreationThemeAdapter(Context context, SiteCreationListener siteCreationListener) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);

        mSiteCreationListener = siteCreationListener;
    }

    public void setData(boolean isLoading, List<ThemeModel> themes, @StringRes int errorMessage) {
        mIsLoading = isLoading;
        mThemes = themes;
        mErrorMessage = errorMessage;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.site_creation_theme_header,
                    parent, false);
            return new HeaderViewHolder(itemView);
        } else {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.site_creation_theme_item, parent,
                    false);
            return new ThemeViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_HEADER) {
            final HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.progressContainer.setVisibility(mIsLoading || mThemes == null ? View.VISIBLE : View.GONE);
            headerViewHolder.progress.setVisibility(mIsLoading ? View.VISIBLE : View.GONE);
            if (!mIsLoading && mThemes == null) {
                // this is an error situation so, show an error
                headerViewHolder.label.setText(mErrorMessage);
            } else {
                headerViewHolder.label.setText(null);
            }
        } else {
            final ThemeModel theme = getItem(position);
            final ThemeViewHolder themeViewHolder = (ThemeViewHolder) holder;
            themeViewHolder.nameView.setText(theme.getName());
            configureImageView(themeViewHolder, theme.getScreenshotUrl());

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mSiteCreationListener.withTheme(theme.getThemeId());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return 1 + (mThemes == null ? 0 : mThemes.size());
    }

    @Override
    public long getItemId(int position) {
        return position == 0 ? RecyclerView.NO_ID : getItem(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    private ThemeModel getItem(int position) {
        return mThemes.get(position - 1);
    }

    private static final String THEME_IMAGE_PARAMETER = "?w=";

    private void configureImageView(ThemeViewHolder themeViewHolder, String screenshotURL) {
        String requestURL = (String) themeViewHolder.imageView.getTag();
        if (requestURL == null) {
            requestURL = screenshotURL;
            themeViewHolder.imageView.setDefaultImageResId(R.drawable.theme_loading);
            themeViewHolder.imageView.showDefaultImage(); // force showing the default image so layout is computed
            themeViewHolder.imageView.setTag(requestURL);
        }

        if (!requestURL.equals(screenshotURL)) {
            requestURL = screenshotURL;
        }

        int mViewWidth = AppPrefs.getThemeImageSizeWidth();
        themeViewHolder.imageView.setImageUrl(requestURL + THEME_IMAGE_PARAMETER + mViewWidth,
                WPNetworkImageView.ImageType.PHOTO);
    }
}
