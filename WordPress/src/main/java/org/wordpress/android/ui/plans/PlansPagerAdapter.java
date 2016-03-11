package org.wordpress.android.ui.plans;

import android.app.FragmentManager;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import org.wordpress.android.ui.plans.models.SitePlan;
import org.wordpress.android.util.AppLog;

/**
 * ViewPager adapter for the main plans activity
 */
class PlansPagerAdapter extends FragmentPagerAdapter {
    private final SitePlan[] mSitePlans;
    private final SparseArray<PlanFragment> mFragmentMap = new SparseArray<>();

    public PlansPagerAdapter(FragmentManager fm, @NonNull SitePlan[] sitePlans) {
        super(fm);
        mSitePlans = sitePlans.clone();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (isValidPosition(position)) {
            PlanFragment fragment = mFragmentMap.get(position);
            if (fragment == null) {
                fragment = getItem(position);
            }
            return fragment.getTitle();
        }
        return super.getPageTitle(position);
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
    public Object instantiateItem(ViewGroup container, int position) {
        Object item = super.instantiateItem(container, position);
        if (item instanceof PlanFragment) {
            mFragmentMap.put(position, (PlanFragment) item);
        }
        return item;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        mFragmentMap.remove(position);
        super.destroyItem(container, position, object);
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
