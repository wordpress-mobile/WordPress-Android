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
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.plans.models.Feature;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.plans.models.SitePlan;
import org.wordpress.android.util.ToastUtils;

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

        // add the list of features
        List<Feature> features = PlansUtils.getPlanFeatures(mSitePlan.getProductID());
        if (features != null) {
            for (Feature feature : features) {
                addFeature(feature);
            }
        }

        // show purchase button when appropriate
        boolean showPurchaseButton;
        if (mSitePlan.isCurrentPlan()) {
            showPurchaseButton = false;
        } else {
            long currentPlanId = WordPress.wpDB.getPlanIdForLocalTableBlogId((int)mSitePlan.getBlogLocalTableID());
            long thisPlanId = mSitePlan.getProductID();
            if (currentPlanId == PlansConstants.FREE_PLAN_ID) {
                showPurchaseButton = true;
            } else if (currentPlanId == PlansConstants.PREMIUM_PLAN_ID) {
                showPurchaseButton = (thisPlanId == PlansConstants.FREE_PLAN_ID);
            } else if (currentPlanId == PlansConstants.BUSINESS_PLAN_ID) {
                showPurchaseButton = false;
            } else if (currentPlanId == PlansConstants.JETPACK_FREE_PLAN_ID) {
                showPurchaseButton = true;
            } else if (currentPlanId == PlansConstants.JETPACK_PREMIUM_PLAN_ID) {
                showPurchaseButton = (thisPlanId == PlansConstants.JETPACK_BUSINESS_PLAN_ID);
            } else if (currentPlanId == PlansConstants.JETPACK_BUSINESS_PLAN_ID) {
                showPurchaseButton = false;
            } else {
                showPurchaseButton = true;
            }

        }

        ViewGroup framePurchase = (ViewGroup) getView().findViewById(R.id.frame_purchase);
        TextView txtPurchase = (TextView) framePurchase.findViewById(R.id.text_purchase);
        if (showPurchaseButton) {
            String purchase = mPlanDetails.getFormattedPrice()
                    + " | <b>"
                    + getActivity().getString(R.string.plan_purchase_now)
                    + "</b>";
            txtPurchase.setText(Html.fromHtml(purchase));
            txtPurchase.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ToastUtils.showToast(v.getContext(), "Not implemented yet");
                }
            });
            framePurchase.setVisibility(View.VISIBLE);
        } else {
            framePurchase.setVisibility(View.GONE);
        }
    }

    private void addFeature(Feature feature) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.plan_feature_item, mPlanContainerView, false);

        TextView txtTitle = (TextView) view.findViewById(R.id.text_feature_title);
        TextView txtDescription = (TextView) view.findViewById(R.id.text_feature_description);
        txtTitle.setText(feature.getTitle());
        txtDescription.setText(feature.getDescription());

        mPlanContainerView.addView(view);
    }

    private void setSitePlan(SitePlan sitePlan) {
        mSitePlan = sitePlan;
        mPlanDetails = PlansUtils.getGlobalPlan(mSitePlan.getProductID());
    }

    public SitePlan getSitePlan() {
        return mSitePlan;
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
