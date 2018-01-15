package org.wordpress.android.ui.accounts.signup;

import android.content.Context;
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

public class SiteCreationThemeAdapter extends RecyclerView.Adapter<SiteCreationThemeAdapter.ThemeViewHolder> {
    private List<ThemeModel> mThemes;
    private SiteCreationListener mSiteCreationListener;

    public static class ThemeViewHolder extends RecyclerView.ViewHolder {
        private final WPNetworkImageView imageView;
        private final TextView nameView;

        public ThemeViewHolder(View view) {
            super(view);
            imageView = (WPNetworkImageView) view.findViewById(R.id.theme_grid_item_image);
            nameView = (TextView) view.findViewById(R.id.theme_grid_item_name);
        }
    }

    public SiteCreationThemeAdapter(Context context, SiteCreationListener siteCreationListener,
            List<ThemeModel> themes) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);

        mSiteCreationListener = siteCreationListener;
        mThemes = themes;
    }

    @Override
    public ThemeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.site_creation_theme_item, parent, false);
        return new ThemeViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ThemeViewHolder holder, int position) {
        final ThemeModel theme = mThemes.get(position);

        holder.nameView.setText(theme.getName());
        configureImageView(holder, theme.getScreenshotUrl());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSiteCreationListener.withTheme(theme.getThemeId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mThemes.size();
    }

    private static final String THEME_IMAGE_PARAMETER = "?w=";

    private void configureImageView(ThemeViewHolder themeViewHolder, String screenshotURL) {
        String requestURL = (String) themeViewHolder.imageView.getTag();
        if (requestURL == null) {
            requestURL = screenshotURL;
            themeViewHolder.imageView.setDefaultImageResId(R.drawable.theme_loading);
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
