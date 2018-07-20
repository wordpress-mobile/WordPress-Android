package org.wordpress.android.ui.accounts.signup;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.List;

import javax.inject.Inject;

public class SiteCreationThemeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private boolean mIsLoading;
    private List<ThemeModel> mThemes;
    private @StringRes int mErrorMessage;
    private SiteCreationListener mSiteCreationListener;

    @Inject ImageManager mImageManager;

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public final View progressContainer;
        public final View progress;
        public final TextView label;

        HeaderViewHolder(View itemView) {
            super(itemView);
            this.progressContainer = itemView.findViewById(R.id.progress_container);
            this.progress = itemView.findViewById(R.id.progress_bar);
            this.label = itemView.findViewById(R.id.progress_label);
        }
    }

    public static class ThemeViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mImageView;
        private final TextView mNameView;

        ThemeViewHolder(View view) {
            super(view);
            mImageView = view.findViewById(R.id.theme_grid_item_image);
            mNameView = view.findViewById(R.id.theme_grid_item_name);
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
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
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
            themeViewHolder.mNameView.setText(theme.getName());
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
        int mViewWidth = AppPrefs.getThemeImageSizeWidth();
        mImageManager
                .load(themeViewHolder.mImageView, ImageType.THEME, screenshotURL + THEME_IMAGE_PARAMETER + mViewWidth,
                        ScaleType.FIT_CENTER);
    }
}
