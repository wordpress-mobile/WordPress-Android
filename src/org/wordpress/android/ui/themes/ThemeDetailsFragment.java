package org.wordpress.android.ui.themes;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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
    private ThemeDetailsFragmentCallback mCallback;
    private String mPreviewURL;
    private Button mActivateThemeButton;
    
    public interface ThemeDetailsFragmentCallback {
        public void onResumeThemeDetailsFragment();
        public void onPauseThemeDetailsFragment();
        public void onLivePreviewClicked(String themeId, String previewURL);
        public void onActivateThemeClicked(String themeId);
    }
    
    private String getThemeId() {
        if (getArguments() != null)
            return getArguments().getString(ARGS_THEME_ID);
        else
            return null;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        try {
            mCallback = (ThemeDetailsFragmentCallback) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ThemeDetailsFragmentCallback");
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.theme_details_fragment, container, false);
        
        mNameView = (TextView) view.findViewById(R.id.theme_details_fragment_name);
        mImageView = (NetworkImageView) view.findViewById(R.id.theme_details_fragment_image);
        mDescriptionView = (TextView) view.findViewById(R.id.theme_details_fragment_details_description);
        
        mLivePreviewButton = (Button) view.findViewById(R.id.theme_details_fragment_preview_button);
        mActivateThemeButton = (Button) view.findViewById(R.id.theme_details_fragment_activate_button);
        mLivePreviewButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (mPreviewURL != null)
                    mCallback.onLivePreviewClicked(getThemeId(), mPreviewURL);
            }
        });
        
        mActivateThemeButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                String themeId = getThemeId();
                if (themeId != null) {
                    mCallback.onActivateThemeClicked(themeId);
                }
                
            }
        });
        
        loadTheme(getThemeId());
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();      
        mCallback.onResumeThemeDetailsFragment();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mCallback.onPauseThemeDetailsFragment();
    };

    public void loadTheme(String themeId) {
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        Theme theme = WordPress.wpDB.getTheme(blogId, themeId);
        if (theme != null) {
            mNameView.setText(theme.getName());
            mImageView.setImageUrl(theme.getScreenshotURL(), WordPress.imageLoader);
            mDescriptionView.setText(Html.fromHtml(theme.getDescription()));
            mDescriptionView.setMovementMethod(LinkMovementMethod.getInstance());
            mPreviewURL = theme.getPreviewURL();
        }
    }
    
}
