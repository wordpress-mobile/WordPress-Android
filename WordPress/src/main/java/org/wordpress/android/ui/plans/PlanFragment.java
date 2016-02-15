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
import org.wordpress.android.ui.plans.models.SitePlan;

import java.util.List;

public class PlanFragment extends Fragment {
    private static final String PLAN = "PLAN";
    private static final String PLAN_DETAILS = "PLAN_DETAILS";

    private ViewGroup mPlanDetailsOuterContainer;

    private SitePlan mCurrentSitePlan;
    private Plan mCurrentPlanDetails;

    public static PlanFragment newInstance(SitePlan plan) {
        PlanFragment fragment = new PlanFragment();
        fragment.setSitePlan(plan);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(PLAN)) {
                mCurrentSitePlan = (SitePlan) savedInstanceState.getSerializable(PLAN);
            }
            if (savedInstanceState.containsKey(PLAN_DETAILS)) {
                mCurrentPlanDetails = (Plan) savedInstanceState.getSerializable(PLAN_DETAILS);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.plan_fragment, container, false);
        mPlanDetailsOuterContainer = (LinearLayout) rootView.findViewById(R.id.plan_outer_container);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPlanUI();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(PLAN, mCurrentSitePlan);
        outState.putSerializable(PLAN_DETAILS, mCurrentPlanDetails);
        super.onSaveInstanceState(outState);
    }

    private void refreshPlanUI() {
        if (!isAdded()) {
            return;
        }
        if (mCurrentSitePlan == null) {
            // TODO This should never happen - Fix this. Close the activity?
            return;
        }

        ImageView imgPlan = (ImageView) mPlanDetailsOuterContainer.findViewById(R.id.plan_icon);
        int pictureResID = PlansUIHelper.getPrimaryImageResIDForPlan(mCurrentSitePlan.getProductID());
        if (pictureResID == PlansUIHelper.NO_PICTURE_FOR_PLAN_RES_ID) {
            imgPlan.setVisibility(View.GONE);
        } else {
            imgPlan.setImageDrawable(getResources().getDrawable(pictureResID));
        }

        // show product short name in bold, ex: "WordPress.com <b>Premium</b>"
        TextView txtProductName = (TextView) mPlanDetailsOuterContainer.findViewById(R.id.text_product_name);
        String productShortName = mCurrentPlanDetails.getProductNameShort();
        String productName = mCurrentPlanDetails.getProductName().replace(productShortName,
                "<b>" + productShortName + "</b>");
        txtProductName.setText(Html.fromHtml(productName));

        TextView txtTagLine = (TextView) mPlanDetailsOuterContainer.findViewById(R.id.text_tagline);
        txtTagLine.setText(mCurrentPlanDetails.getTagline());

        List<Feature> features = PlansUtils.getPlanFeatures(mCurrentSitePlan.getProductID());
        if (features != null) {
            for (Feature feature : features) {
                addFeature(feature);
            }
        }

    }

    private void addFeature(Feature feature) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.plan_feature_item, mPlanDetailsOuterContainer, false);

        TextView txtTitle = (TextView) view.findViewById(R.id.text_feature_title);
        TextView txtDescription = (TextView) view.findViewById(R.id.text_feature_description);
        txtTitle.setText(feature.getTitle());
        txtDescription.setText(feature.getDescription());

        mPlanDetailsOuterContainer.addView(view);
    }

    public void setSitePlan(SitePlan plan) {
        mCurrentSitePlan = plan;
        mCurrentPlanDetails = PlansUtils.getGlobalPlan(mCurrentSitePlan.getProductID());
    }

    public SitePlan getSitePlan() {
        return mCurrentSitePlan;
    }

    private static final String UNICODE_CHECKMARK = "\u2713";
    String getTitle() {
        if (mCurrentSitePlan.isCurrentPlan()) {
            return UNICODE_CHECKMARK + " " + mCurrentPlanDetails.getProductNameShort();
        } else {
            return mCurrentPlanDetails.getProductNameShort();
        }
    }
}
