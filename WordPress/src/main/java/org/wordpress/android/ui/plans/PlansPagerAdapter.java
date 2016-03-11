package org.wordpress.android.ui.plans;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import java.util.List;

/**
 * ViewPager adapter for the main plans activity
 */
class PlansPagerAdapter extends FragmentPagerAdapter {
    private final List<Fragment> mFragments;

    PlansPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        mFragments = fragments;
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
