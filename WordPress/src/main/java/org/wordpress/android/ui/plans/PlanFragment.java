package org.wordpress.android.ui.plans;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.plans.models.Feature;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.plans.models.PlanFeaturesHighlightSection;
import org.wordpress.android.ui.plans.models.SitePlan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        txtTagLine.setText(mPlanDetails.getTagline());

        // The current plan could probably have some features to highlight on the details screen
        HashMap<String, Feature> globalFeatures = PlansUtils.getFeatures();
        ArrayList<PlanFeaturesHighlightSection> sectionsToHighlight = mPlanDetails.getFeaturesHighlightSections();
        if (globalFeatures != null && sectionsToHighlight != null) {
            for (PlanFeaturesHighlightSection currentSection: sectionsToHighlight) {
                String sectionTitle = currentSection.getTitle(); // section title could be empty.
                // TODO: add section title on the screen
                ArrayList<String> featuresToHighlight = currentSection.getFeatures(); // features to highlight in this section
                for (String currentFeatureSlug: featuresToHighlight) {
                    Feature currentFeature = globalFeatures.get(currentFeatureSlug);
                    if (currentFeature != null) {
                        addFeature(currentFeature);
                    }
                }
            }
        }
    }

    private void addFeature(Feature feature) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.plan_feature_item, mPlanContainerView, false);

        TextView txtTitle = (TextView) view.findViewById(R.id.text_feature_title);
        TextView txtDescription = (TextView) view.findViewById(R.id.text_feature_description);
        txtTitle.setText(feature.getTitleForPlan(mPlanDetails.getProductID()));
        txtDescription.setText(feature.getDescriptionForPlan(mPlanDetails.getProductID()));

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
