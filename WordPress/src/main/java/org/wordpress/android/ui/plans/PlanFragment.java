package org.wordpress.android.ui.plans;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.wordpress.android.R;
import org.wordpress.android.ui.plans.models.Feature;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.plans.models.SitePlan;
import org.wordpress.android.widgets.WPTextView;

import java.util.List;

public class PlanFragment extends Fragment {
    private static final String PLAN = "PLAN";
    private static final String PLAN_DETAILS = "PLAN_DETAILS";

    private LinearLayout mPlanDetailsOuterContainer;
    private ImageView mPlanImageView;

    private SitePlan mCurrentSitePlan;
    private Plan mCurrentPlanDetails;

    public static PlanFragment newInstance() {
        return new PlanFragment();
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
        mPlanImageView = (ImageView) rootView.findViewById(R.id.plan_icon);
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

        int pictureResID = PlansUIHelper.getPrimaryImageResIDForPlan(mCurrentSitePlan.getProductID());
        if (pictureResID == PlansUIHelper.NO_PICTURE_FOR_PLAN_RES_ID) {
            mPlanImageView.setVisibility(View.GONE);
        } else {
            mPlanImageView.setImageDrawable(getResources().getDrawable(pictureResID));
        }

        addTextView(mCurrentPlanDetails.getTagline());

        addTextView(PlansUtils.getPlanPriceValue(mCurrentPlanDetails) + PlansUtils.getPlanPriceCurrencySymbol(mCurrentPlanDetails));

        addTextView(mCurrentPlanDetails.getBillPeriodLabel());

        addTextView("");
        addTextView("Features available:");

        List<Feature> features = PlansUtils.getPlanFeatures(mCurrentSitePlan.getProductID());
        if (features != null) {
            for (Feature current : features) {
                addTextView(current.getTitle());
            }
        }

        addTextView("");

        if (mCurrentSitePlan.isCurrentPlan()) {
            addTextView("Your current Plan.");
        } else {
            addTextView("Upgrade to this Plan.");
        }

    }

    private void addTextView(String text) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        WPTextView valueTV = (WPTextView) inflater.inflate(R.layout.plan_text_item, mPlanDetailsOuterContainer, false);
        valueTV.setText(text);
        mPlanDetailsOuterContainer.addView(valueTV);
    }


    public void setSitePlan(SitePlan plan) {
        mCurrentSitePlan = plan;
        mCurrentPlanDetails = PlansUtils.getGlobalPlan(mCurrentSitePlan.getProductID());
    }

    public SitePlan getSitePlan() {
        return mCurrentSitePlan;
    }

    String getTitle() {
        return mCurrentPlanDetails.getProductNameShort();
    }
}
