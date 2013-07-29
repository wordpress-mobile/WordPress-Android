
package org.wordpress.android.ui.themes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.view.Menu;
import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Theme;
import org.wordpress.android.ui.ViewSiteActivity;
import org.wordpress.android.ui.WPActionBarActivity;

public class ThemeDetailsFragment extends SherlockDialogFragment {

    public static final String TAG = ThemeDetailsFragment.class.getName();
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
    private String mPreviewURL;
    private Button mActivateThemeButton;

    private ThemeDetailsFragmentCallback mCallback;
    private Button mViewSiteButton;
    private View mCurrentThemeView;

    public interface ThemeDetailsFragmentCallback {
        public void onResume(Fragment fragment);
        public void onPause(Fragment fragment);
        public void onLivePreviewClicked(String themeId, String previewURL);
        public void onActivateThemeClicked(String themeId, Fragment fragment);
    }

    public String getThemeId() {
        if (getArguments() != null)
            return getArguments().getString(ARGS_THEME_ID);
        else
            return null;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
    public void onStart()
    {
      super.onStart();

      // safety check
      if (getDialog() == null)
        return;

      int dialogWidth = (int) getActivity().getResources().getDimension(R.dimen.theme_details_fragment_width);
      
      int dialogHeight = (int) getActivity().getResources().getDimension(R.dimen.theme_details_fragment_height);

      getDialog().getWindow().setLayout(dialogWidth, dialogHeight);

    }
     
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.theme_details_fragment, container, false);

        mNameView = (TextView) view.findViewById(R.id.theme_details_fragment_name);
        mImageView = (NetworkImageView) view.findViewById(R.id.theme_details_fragment_image);
        mDescriptionView = (TextView) view.findViewById(R.id.theme_details_fragment_details_description);

        mCurrentThemeView = (View) view.findViewById(R.id.theme_details_fragment_current_theme_container);
        
        mLivePreviewButton = (Button) view.findViewById(R.id.theme_details_fragment_preview_button);
        mActivateThemeButton = (Button) view.findViewById(R.id.theme_details_fragment_activate_button);
        
        mViewSiteButton = (Button) view.findViewById(R.id.theme_details_fragment_view_site_button);
        mViewSiteButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ViewSiteActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            }
        });
        
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
                    mCallback.onActivateThemeClicked(themeId, ThemeDetailsFragment.this);
                    mActivateThemeButton.setEnabled(false);
                    mActivateThemeButton.setText(R.string.theme_activating_button);
                }

            }
        });

        loadTheme(getThemeId());

        return view;
    }
    
    public void showViewSite() {
        mLivePreviewButton.setVisibility(View.GONE);
        mActivateThemeButton.setVisibility(View.GONE);
        mViewSiteButton.setVisibility(View.VISIBLE);
        mCurrentThemeView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCallback.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mCallback.onPause(this);
    };

    public void loadTheme(String themeId) {
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        Theme theme = WordPress.wpDB.getTheme(blogId, themeId);
        if (theme != null) {
            if (mNameView != null) {
                mNameView.setText(theme.getName());
            }
            mImageView.setImageUrl(theme.getScreenshotURL(), WordPress.imageLoader);
            mDescriptionView.setText(Html.fromHtml(theme.getDescription()));
            mDescriptionView.setMovementMethod(LinkMovementMethod.getInstance());
            mPreviewURL = theme.getPreviewURL();
            
            if (theme.getIsCurrentTheme()) {
                showViewSite();
            }

            if (getDialog() != null) {
                getDialog().setTitle(theme.getName());
            }
        }

    }
    
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.removeItem(R.id.menu_search);
        menu.removeItem(R.id.menu_refresh);
    }

    public void onThemeActivated(boolean activated) {
        mActivateThemeButton.setEnabled(true);
        mActivateThemeButton.setText(R.string.theme_activate_button);
        
        showViewSite();
    }
}
