package org.wordpress.android.ui.plans;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.plans.models.Feature;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.plans.models.PlanFeaturesHighlightSection;
import org.wordpress.android.ui.plans.models.SitePlan;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.HtmlUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class PlanFragment extends Fragment {
    private static final String PLAN = "PLAN";
    private static final String PLAN_DETAILS = "PLAN_DETAILS";

    private ViewGroup mPlanContainerView;

    private SitePlan mSitePlan;
    private Plan mPlanDetails;

    public static PlanFragment newInstance(SitePlan sitePlan) {
        PlanFragment fragment = new PlanFragment();
        fragment.setSitePlan(sitePlan);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(PLAN)) {
                mSitePlan = (SitePlan) savedInstanceState.getSerializable(PLAN);
            }
            if (savedInstanceState.containsKey(PLAN_DETAILS)) {
                mPlanDetails = (Plan) savedInstanceState.getSerializable(PLAN_DETAILS);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.plan_fragment, container, false);
        mPlanContainerView = (LinearLayout) rootView.findViewById(R.id.plan_container);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPlanUI();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(PLAN, mSitePlan);
        outState.putSerializable(PLAN_DETAILS, mPlanDetails);
        super.onSaveInstanceState(outState);
    }

    private void refreshPlanUI() {
        if (!isAdded()) {
            return;
        }
        if (mSitePlan == null) {
            // TODO This should never happen - Fix this. Close the activity?
            return;
        }

        ImageView imgPlan = (ImageView) mPlanContainerView.findViewById(R.id.plan_icon);
        int pictureResID = PlansUIHelper.getPrimaryImageResIDForPlan(mSitePlan.getProductID());
        if (pictureResID == PlansUIHelper.NO_PICTURE_FOR_PLAN_RES_ID) {
            imgPlan.setVisibility(View.GONE);
        } else {
            imgPlan.setImageDrawable(getResources().getDrawable(pictureResID));
        }

        // show product short name in bold, ex: "WordPress.com <b>Premium</b>"
        TextView txtProductName = (TextView) mPlanContainerView.findViewById(R.id.text_product_name);
        String productShortName = mPlanDetails.getProductNameShort();
        String productName = mPlanDetails.getProductName().replace(productShortName,
                "<b>" + productShortName + "</b>");
        txtProductName.setText(Html.fromHtml(productName));

        TextView txtTagLine = (TextView) mPlanContainerView.findViewById(R.id.text_tagline);
        txtTagLine.setText(HtmlUtils.fastUnescapeHtml(mPlanDetails.getTagline()));

        // The current plan could probably have some features to highlight on the details screen
        addFeaturesToHighlight();

        // container is hidden at design time, so animate it in if it's still hidden
        if (mPlanContainerView.getVisibility() != View.VISIBLE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                revealContainer();
            } else {
                mPlanContainerView.setVisibility(View.VISIBLE);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void revealContainer() {
        if (!isAdded()) return;

        Point pt = DisplayUtils.getDisplayPixelSize(getActivity());
        float startRadius = 0f;
        float endRadius = (float) Math.hypot(pt.x, pt.y);
        int centerX = pt.x / 2;
        int centerY = pt.y / 2;

        Animator anim = ViewAnimationUtils.createCircularReveal(mPlanContainerView, centerX, centerY, startRadius, endRadius);
        anim.setDuration(getActivity().getResources().getInteger(android.R.integer.config_longAnimTime));
        anim.setInterpolator(new AccelerateInterpolator());
        mPlanContainerView.setVisibility(View.VISIBLE);
        anim.start();
    }

    private void addFeaturesToHighlight() {
        HashMap<String, Feature> globalFeatures = PlansUtils.getFeatures();
        if (globalFeatures == null) {
            AppLog.d(AppLog.T.PLANS, "no global features");
            return;
        }

        ArrayList<PlanFeaturesHighlightSection> sectionsToHighlight = mPlanDetails.getFeaturesHighlightSections();
        if (sectionsToHighlight == null) {
            AppLog.d(AppLog.T.PLANS, "no sections to highlight");
            return;
        }

        for (PlanFeaturesHighlightSection section : sectionsToHighlight) {
            // add section title if it's not empty
            addSectionTitle(section.getTitle());
            // add features to highlight in this section
            ArrayList<String> featuresToHighlight = section.getFeatures();
            for (String featureSlug : featuresToHighlight) {
                addFeature(globalFeatures.get(featureSlug));
            }
        }
    }

    private void addSectionTitle(String title) {
        if (TextUtils.isEmpty(title)) return;

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.plan_section_title, mPlanContainerView, false);

        TextView txtTitle = (TextView) view.findViewById(R.id.text_section_title);
        txtTitle.setText(HtmlUtils.fastUnescapeHtml(title));

        mPlanContainerView.addView(view);
    }

    private void addFeature(Feature feature) {
        if (feature == null) return;

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.plan_feature_item, mPlanContainerView, false);

        TextView txtTitle = (TextView) view.findViewById(R.id.text_feature_title);
        TextView txtDescription = (TextView) view.findViewById(R.id.text_feature_description);
        String title = HtmlUtils.fastUnescapeHtml(feature.getTitleForPlan(mPlanDetails.getProductID()));
        String description = HtmlUtils.fastUnescapeHtml(feature.getDescriptionForPlan(mPlanDetails.getProductID()));
        txtTitle.setText(title);
        txtDescription.setText(description);

        // TODO: right now icon is always empty, so we show noticon_publish as a placeholder
        NetworkImageView imgIcon = (NetworkImageView) view.findViewById(R.id.image_icon);
        String iconUrl = feature.getIconForPlan(mPlanDetails.getProductID());
        if (!TextUtils.isEmpty(iconUrl)) {
            imgIcon.setImageUrl(iconUrl, WordPress.imageLoader);
        } else {
            imgIcon.setDefaultImageResId(R.drawable.noticon_publish);
        }

        mPlanContainerView.addView(view);
    }

    private void setSitePlan(SitePlan sitePlan) {
        mSitePlan = sitePlan;
        mPlanDetails = PlansUtils.getGlobalPlan(mSitePlan.getProductID());
    }

    public SitePlan getSitePlan() {
        return mSitePlan;
    }

    public Plan getPlanDetails() {
        return mPlanDetails;
    }

    private static final String UNICODE_CHECKMARK = "\u2713";
    String getTitle() {
        if (mSitePlan.isCurrentPlan()) {
            return UNICODE_CHECKMARK + " " + mPlanDetails.getProductNameShort();
        } else {
            return mPlanDetails.getProductNameShort();
        }
    }
}
