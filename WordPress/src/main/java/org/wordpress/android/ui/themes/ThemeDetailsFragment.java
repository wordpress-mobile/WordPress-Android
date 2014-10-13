package org.wordpress.android.ui.themes;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Theme;
import org.wordpress.android.ui.ViewSiteActivity;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.WPLinkMovementMethod;
import org.wordpress.android.analytics.AnalyticsTracker;

import java.util.ArrayList;

/**
 * A fragment to show the theme's details, including its description and features.
 */
public class ThemeDetailsFragment extends DialogFragment {
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
    private View mActivatingProgressView;
    private FrameLayout mActivateThemeContainer;
    private View mPremiumThemeView;
    private LinearLayout mFeaturesContainer;
    private View mLeftContainer;
    private View mParentView;

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

        // retain this fragment across configuration changes
        setRetainInstance(true);
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
    public void onDetach() {
        super.onDetach();
        // set callback to null so we don't accidentally leak the activity instance
        mCallback = null;
    }

    private boolean hasCallback() {
        return (mCallback != null);
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
        mParentView = inflater.inflate(R.layout.theme_details_fragment, container, false);

        mNameView = (TextView) mParentView.findViewById(R.id.theme_details_fragment_name);
        mImageView = (NetworkImageView) mParentView.findViewById(R.id.theme_details_fragment_image);
        mDescriptionView = (TextView) mParentView.findViewById(R.id.theme_details_fragment_details_description);

        mCurrentThemeView = (View) mParentView.findViewById(R.id.theme_details_fragment_current_theme_text);
        mPremiumThemeView = (View) mParentView.findViewById(R.id.theme_details_fragment_premium_theme_text);

        mLivePreviewButton = (Button) mParentView.findViewById(R.id.theme_details_fragment_preview_button);
        mActivateThemeButton = (Button) mParentView.findViewById(R.id.theme_details_fragment_activate_button);
        mActivatingProgressView = (View) mParentView.findViewById(R.id.theme_details_fragment_activating_progress);
        mActivateThemeContainer = (FrameLayout) mParentView.findViewById(R.id.theme_details_fragment_activate_button_container);

        mFeaturesContainer = (LinearLayout) mParentView.findViewById(R.id.theme_details_fragment_features_container);
        mLeftContainer = (View) mParentView.findViewById(R.id.theme_details_fragment_left_container);

        mViewSiteButton = (Button) mParentView.findViewById(R.id.theme_details_fragment_view_site_button);
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
                if (mPreviewURL != null && hasCallback()) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.THEMES_PREVIEWED_SITE);
                    mCallback.onLivePreviewClicked(getThemeId(), mPreviewURL);
                }
            }
        });

        mActivateThemeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String themeId = getThemeId();
                if (themeId != null) {
                    if (hasCallback())
                        mCallback.onActivateThemeClicked(themeId, ThemeDetailsFragment.this);
                    setIsActivatingTheme(true);
                    AnalyticsTracker.track(AnalyticsTracker.Stat.THEMES_CHANGED_THEME);
                }

            }
        });

        loadTheme(getThemeId());

        return mParentView;
    }

    public void showViewSite() {
        mLivePreviewButton.setVisibility(View.GONE);
        mActivateThemeContainer.setVisibility(View.GONE);
        mViewSiteButton.setVisibility(View.VISIBLE);
        mCurrentThemeView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasCallback())
            mCallback.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (hasCallback())
            mCallback.onPause(this);
    };

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.removeItem(R.id.menu_search);
    }

    /*
     * update views to indicate that a theme is being activated, or has finished being activated
     */
    protected void setIsActivatingTheme(boolean isActivating) {
        if (isActivating) {
            mActivateThemeButton.setEnabled(false);
            mActivateThemeButton.setText("");
            mActivatingProgressView.setVisibility(View.VISIBLE);
        } else {
            mActivateThemeButton.setEnabled(true);
            mActivateThemeButton.setText(R.string.theme_activate_button);
            mActivatingProgressView.setVisibility(View.GONE);
        }
    }

    public void onThemeActivated(boolean activated) {
        setIsActivatingTheme(false);
        if (activated)
            showViewSite();
    }

    public void loadTheme(String themeId) {
        String blogId = String.valueOf(WordPress.getCurrentBlog().getRemoteBlogId());
        Theme theme = WordPress.wpDB.getTheme(blogId, themeId);
        if (theme != null) {
            if (mNameView != null) {
                mNameView.setText(theme.getName());
            }
            mImageView.setImageUrl(theme.getScreenshotURL(), WordPress.imageLoader);
            mDescriptionView.setText(Html.fromHtml(theme.getDescription()));
            mDescriptionView.setMovementMethod(WPLinkMovementMethod.getInstance());
            mPreviewURL = theme.getPreviewURL();

            loadFeatureView(theme.getFeaturesArray());
            if (theme.isPremium()) {
                mPremiumThemeView.setVisibility(View.VISIBLE);
            } else {
                mPremiumThemeView.setVisibility(View.GONE);
            }

            if (theme.isCurrent()) {
                showViewSite();
            }

            if (getDialog() != null) {
                getDialog().setTitle(theme.getName());
            }
        }

    }

    private void loadFeatureView(ArrayList<String> featuresArray) {
        int size = featuresArray.size();
        View views[] = new View[size];

        LayoutInflater inflater = LayoutInflater.from(getActivity());

        for (int i = 0; i < size; i++) {
             TextView tv = (TextView) inflater.inflate(R.layout.theme_feature_text, mFeaturesContainer, false);
             tv.setText(featuresArray.get(i));
             views[i] = tv;
        }

        // make the list of features appear in such a way that the text appear on the next line
        // when reaching the end of the current line
        populateViews(mFeaturesContainer, views, getActivity());
    }

    /**
     * Copyright 2011 Sherif
     * Updated by Karim Varela to handle LinearLayouts with other views on either side.
     * @param linearLayout
     * @param views : The views to wrap within LinearLayout
     * @param context
     * @author Karim Varela
     **/
    private void populateViews(LinearLayout linearLayout, View[] views, Context context) {
        RelativeLayout.LayoutParams llParams =
                (android.widget.RelativeLayout.LayoutParams) linearLayout.getLayoutParams();

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        linearLayout.removeAllViews();

        int maxWidth = display.getWidth() - llParams.leftMargin - llParams.rightMargin - mParentView.getPaddingLeft()
                - mParentView.getPaddingRight();

        if (DisplayUtils.isXLarge(getActivity())) {
            int minDialogWidth = getResources().getDimensionPixelSize(R.dimen.theme_details_dialog_min_width);
            int dialogWidth = Math.max((int) (display.getWidth() * 0.6), minDialogWidth);
            maxWidth = dialogWidth / 2 - llParams.leftMargin - llParams.rightMargin;
        } else if (mLeftContainer != null && mLeftContainer.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            mLeftContainer.measure(spec, spec);
            LinearLayout.LayoutParams params = (LayoutParams) mLeftContainer.getLayoutParams();
            maxWidth -= mLeftContainer.getMeasuredWidth() + params.rightMargin + params.leftMargin;
        }

        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams params;
        LinearLayout newLL = new LinearLayout(context);
        newLL.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        newLL.setGravity(Gravity.LEFT);
        newLL.setOrientation(LinearLayout.HORIZONTAL);

        int widthSoFar = 0;

        int dp4 = DisplayUtils.dpToPx(getActivity(), 4);
        int dp2 = DisplayUtils.dpToPx(getActivity(), 2);

        for (int i = 0; i < views.length; i++)
        {
            LinearLayout LL = new LinearLayout(context);
            LL.setOrientation(LinearLayout.HORIZONTAL);
            LL.setLayoutParams(new ListView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            views[i].measure(0, 0);
            params = new LinearLayout.LayoutParams(views[i].getMeasuredWidth(), LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dp2, dp4, dp2);

            LL.addView(views[i], params);
            LL.measure(0, 0);
            widthSoFar += views[i].getMeasuredWidth() + views[i].getPaddingLeft() + views[i].getPaddingRight();
            if (widthSoFar >= maxWidth)
            {
                linearLayout.addView(newLL);

                newLL = new LinearLayout(context);
                newLL.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                newLL.setOrientation(LinearLayout.HORIZONTAL);
                newLL.setGravity(Gravity.LEFT);
                params = new LinearLayout.LayoutParams(LL.getMeasuredWidth(), LL.getMeasuredHeight());
                newLL.addView(LL, params);
                widthSoFar = LL.getMeasuredWidth();
            }
            else
            {
                newLL.addView(LL);
            }
        }
        linearLayout.addView(newLL);
    }
}
