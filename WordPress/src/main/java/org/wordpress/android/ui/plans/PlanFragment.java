package org.wordpress.android.ui.plans;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.plans.models.SitePlan;

public class PlanFragment extends Fragment {
    private static final String PLAN = "PLAN";

    private TextView mPlanDetailsRawTextView;

    private SitePlan mCurrentPlan;

    public static PlanFragment newInstance() {
        return new PlanFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(PLAN)) {
                mCurrentPlan = (SitePlan) savedInstanceState.getSerializable(PLAN);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.plan_fragment, container, false);
        mPlanDetailsRawTextView = (TextView) rootView.findViewById(R.id.plan_details_raw);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPlanUI();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(PLAN, mCurrentPlan);
        super.onSaveInstanceState(outState);
    }

    private void refreshPlanUI() {
        if (mCurrentPlan!= null) {
            mPlanDetailsRawTextView.setText(mCurrentPlan.getProductName() + " " + mCurrentPlan.getFormattedPrice());
        }
    }

    public void setPlan(SitePlan plan) {
        mCurrentPlan = plan;
    }

    public SitePlan getPlan() {
        return mCurrentPlan;
    }
}
