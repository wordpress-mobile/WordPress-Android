package org.wordpress.android.ui.plans.adapters;

import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;

import org.wordpress.android.ui.plans.PlanFragment;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.util.AppLog;

/**
 * ViewPager adapter for the main plans activity
 */
public class PlansPagerAdapter extends FragmentPagerAdapter {
    private final Plan[] mSitePlans;
    private static final String UNICODE_CHECKMARK = "\u2713";

    public PlansPagerAdapter(FragmentManager fm, @NonNull Plan[] sitePlans) {
        super(fm);
        mSitePlans = sitePlans.clone();
    }

    @Override
    public PlanFragment getItem(int position) {
        return PlanFragment.newInstance(mSitePlans[position]);
    }

    @Override
    public int getCount() {
        return mSitePlans.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (isValidPosition(position)) {
            Plan planDetails = mSitePlans[position];
            if (planDetails == null) {
                AppLog.w(AppLog.T.PLANS, "plans pager > empty plan details in getPageTitle");
                return "";
            } else if (mSitePlans[position].isCurrentPlan()) {
                return UNICODE_CHECKMARK + " " + planDetails.getProductNameShort();
            } else {
                return planDetails.getProductNameShort();
            }
        }
        return super.getPageTitle(position);
    }

    public boolean isValidPosition(int position) {
        return (position >= 0 && position < getCount());
    }

    public int getPositionOfPlan(long planID) {
        for (int i = 0; i < getCount(); i++) {
            if (mSitePlans[i].getProductID() == planID) {
                return i;
            }
        }
        return -1;
    }

    public Plan getPlan(int position) {
        if (isValidPosition(position)) {
            return mSitePlans[position];
        }
        return null;
    }
}
