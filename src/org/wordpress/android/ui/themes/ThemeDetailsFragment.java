package org.wordpress.android.ui.themes;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Theme;

public class ThemeDetailsFragment extends Fragment {

    private static final String ARGS_THEME_ID = "ARGS_THEME_ID";

    public static ThemeDetailsFragment newInstance(String themeId) {
        ThemeDetailsFragment fragment = new ThemeDetailsFragment();
        
        Bundle args = new Bundle();
        args.putString(ARGS_THEME_ID, themeId);
        fragment.setArguments(args);

        return fragment;
    }

    private TextView mNameView;
    private NetworkImageView mImageView;
    private TextView mDescriptionView;
    private Button mLivePreviewButton;
    
    private String getThemeId() {
        if (getArguments() != null)
            return getArguments().getString(ARGS_THEME_ID);
        else
            return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.theme_details_fragment, container, false);
        
        mNameView = (TextView) view.findViewById(R.id.theme_details_fragment_name);
        mImageView = (NetworkImageView) view.findViewById(R.id.theme_details_fragment_image);
        mDescriptionView = (TextView) view.findViewById(R.id.theme_details_fragment_details_description);
        
        mLivePreviewButton = (Button) view.findViewById(R.id.theme_details_fragment_button);
        
        loadTheme(getThemeId());
        
        return view;
    }

    public void loadTheme(String themeId) {
        Theme theme = WordPress.wpDB.getTheme(themeId);
        if (theme != null) {
            mNameView.setText(theme.getName());
            mImageView.setImageUrl(theme.getScreenshotURL(), WordPress.imageLoader);
            mDescriptionView.setText(theme.getDescription());
        }
    }
    
}
