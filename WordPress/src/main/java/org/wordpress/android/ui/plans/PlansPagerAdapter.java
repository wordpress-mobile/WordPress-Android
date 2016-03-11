package org.wordpress.android.ui.plans;

import android.app.FragmentManager;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;

import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.plans.models.SitePlan;
import org.wordpress.android.util.AppLog;

/**
 * ViewPager adapter for the main plans activity
 */
class PlansPagerAdapter extends FragmentPagerAdapter {
    private final SitePlan[] mSitePlans;
    private static final String UNICODE_CHECKMARK = "\u2713";

    public PlansPagerAdapter(FragmentManager fm, @NonNull SitePlan[] sitePlans) {
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

    @Override
    public CharSequence getPageTitle(int position) {
        if (isValidPosition(position)) {
            Plan planDetails = PlansUtils.getGlobalPlan(mSitePlans[position].getProductID());
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

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        // work around "Fragement no longer exists for key" Android bug
        // https://code.google.com/p/android/issues/detail?id=42601
        try {
            AppLog.d(AppLog.T.PLANS, "plans pager > adapter restoreState");
            super.restoreState(state, loader);
        } catch (IllegalStateException e) {
            AppLog.e(AppLog.T.PLANS, e);
        }
    }

    @Override
    public Parcelable saveState() {
        AppLog.d(AppLog.T.PLANS, "plans pager > adapter saveState");
        return super.saveState();
    }
}
