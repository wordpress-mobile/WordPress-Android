package org.wordpress.android.ui.plans;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;

import org.wordpress.android.ui.plans.models.SitePlan;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPager adapter for the main plans activity
 */
class PlansPagerAdapter extends FragmentPagerAdapter {
    private final List<Fragment> mFragments = new ArrayList<>();
    private final SitePlan[] mSitePlans;

    public PlansPagerAdapter(FragmentManager fm, @NonNull SitePlan[] sitePlans) {
        super(fm);
        mSitePlans = sitePlans.clone();
        for (SitePlan plan : mSitePlans) {
            mFragments.add(PlanFragment.newInstance(plan));
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (mFragments != null && isValidPosition(position)) {
            return ((PlanFragment) mFragments.get(position)).getTitle();
        }
        return super.getPageTitle(position);
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    public boolean isValidPosition(int position) {
        return (position >= 0 && position < getCount());
    }

    public int getPositionOfPlan(long planID) {
        for (int i = 0; i < getCount(); i++) {
            PlanFragment fragment = (PlanFragment) getItem(i);
            if (fragment.getSitePlan().getProductID() == planID) {
                return i;
            }
        }
        return -1;
    }
}
