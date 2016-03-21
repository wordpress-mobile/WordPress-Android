package org.wordpress.android.ui.plans;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.themes.ThemeWebActivity;
import org.wordpress.android.util.AppLog;

/**
 * single page within the post-purchase activity's ViewPager
 */
public class PlanPostPurchaseFragment extends Fragment {

    private static final String ARG_PAGE_NUMBER = "page_number";
    private int mPageNumber;

    private static final int PAGE_NUMBER_INTRO = 0;
    private static final int PAGE_NUMBER_CUSTOMIZE = 1;
    private static final int PAGE_NUMBER_VIDEO = 2;
    private static final int PAGE_NUMBER_THEMES = 3;

    static PlanPostPurchaseFragment newInstance(int pageNumber) {
        PlanPostPurchaseFragment fragment = new PlanPostPurchaseFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE_NUMBER, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        mPageNumber = args.getInt(ARG_PAGE_NUMBER);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_PAGE_NUMBER, mPageNumber);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mPageNumber = savedInstanceState.getInt(ARG_PAGE_NUMBER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.plan_post_purchase_fragment, container, false);

        ImageView image = (ImageView) rootView.findViewById(R.id.image);
        TextView txtTitle = (TextView) rootView.findViewById(R.id.text_title);
        TextView txtDescription = (TextView) rootView.findViewById(R.id.text_description);
        Button button = (Button) rootView.findViewById(R.id.button);

        int titleResId;
        int textResId;
        int buttonResId;
        switch (mPageNumber) {
            case PAGE_NUMBER_INTRO:
                titleResId = R.string.plans_post_purchase_title_intro;
                textResId = R.string.plans_post_purchase_text_intro;
                buttonResId = 0;
                break;
            case PAGE_NUMBER_CUSTOMIZE:
                titleResId = R.string.plans_post_purchase_title_customize;
                textResId = R.string.plans_post_purchase_text_customize;
                buttonResId = R.string.plans_post_purchase_button_customize;
                break;
            case PAGE_NUMBER_VIDEO:
                titleResId = R.string.plans_post_purchase_title_video;
                textResId = R.string.plans_post_purchase_text_video;
                buttonResId = R.string.plans_post_purchase_button_video;
                break;
            case PAGE_NUMBER_THEMES:
                titleResId = R.string.plans_post_purchase_title_themes;
                textResId = R.string.plans_post_purchase_text_themes;
                buttonResId = R.string.plans_post_purchase_button_themes;
                break;
            default:
                AppLog.w(AppLog.T.PLANS, "invalid plans post-purchase page");
                throw new IllegalArgumentException("invalid plans post-purchase page");
        }

        txtTitle.setText(titleResId);
        txtDescription.setText(textResId);
        if (buttonResId != 0) {
            button.setVisibility(View.VISIBLE);
            button.setText(buttonResId);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleButtonClick();
                }
            });
        } else {
            button.setVisibility(View.GONE);
        }

        // TODO: image is a placeholder until we have assets
        image.setImageResource(R.drawable.penandink);

        return rootView;
    }

    private void handleButtonClick() {
        switch (mPageNumber) {
            case PAGE_NUMBER_CUSTOMIZE:
                String blogId = WordPress.getCurrentRemoteBlogId();
                String themeId = WordPress.wpDB.getCurrentThemeId(blogId);
                ThemeWebActivity.openTheme(getActivity(), themeId, ThemeWebActivity.ThemeWebActivityType.PREVIEW, true);
                break;
            case PAGE_NUMBER_THEMES:
                ActivityLauncher.viewCurrentBlogThemes(getActivity());
                break;
            case PAGE_NUMBER_VIDEO:
                ActivityLauncher.addNewBlogPostOrPageForResult(getActivity(), WordPress.currentBlog, false);
                break;
        }

        // the user launched another activity, so we close this one
        getActivity().finish();
    }
}
