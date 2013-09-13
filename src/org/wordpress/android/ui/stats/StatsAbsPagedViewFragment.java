package org.wordpress.android.ui.stats;

import java.util.Locale;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.HorizontalTabView;
import org.wordpress.android.ui.HorizontalTabView.Tab;
import org.wordpress.android.ui.HorizontalTabView.TabListener;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.Utils;

/**
 * For stats that have multiple pages (e.g. Today, Yesterday).
 * <p>
 * This fragment appears as a viewpager on phone and as a frame layout with buttons on tablet.
 * Each page is a child fragment.
 * </p>
 * <p>
 * The viewpager's fragments are provided by subclasses implementing {@code getAdapter()}.
 * </p>
 * <p>
 * The tablet fragments are provided by subclasses implementing {@code getFragment(int)}
 * </p>
 */
public abstract class StatsAbsPagedViewFragment extends StatsAbsViewFragment implements TabListener, OnCheckedChangeListener, StatsCursorInterface {

    private static final int ONE_DAY = 24 * 60 * 60 * 1000;

    private static final String SELECTED_BUTTON_INDEX = "SELECTED_BUTTON_INDEX";
    private int mSelectedButtonIndex = 0;
    
    // the active fragment has the tag CHILD_TAG:<mChildIndex>
    private static final String CHILD_TAG = "CHILD_TAG";
    private int mChildIndex = -1;
    
    protected ViewPager mViewPager;
    protected HorizontalTabView mTabView;
    protected FragmentStatePagerAdapter mAdapter;

    private RadioGroup mRadioGroup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_pager_fragment, container, false);
        
        setRetainInstance(true);
        
        if (Utils.isTablet()) {
            initTabletLayout(view);
        } else {
            initPhoneLayout(view);
        }
        
        restoreState(savedInstanceState);
        
        return view;
    }
    
    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;
        
        mSelectedButtonIndex = savedInstanceState.getInt(SELECTED_BUTTON_INDEX);
    }

    private void initTabletLayout(View view) {
        
        TextView titleView = (TextView) view.findViewById(R.id.stats_pager_title);
        titleView.setText(getTitle().toUpperCase(Locale.getDefault()));
        
        String[] titles = getTabTitles();
        
        mRadioGroup = (RadioGroup) view.findViewById(R.id.stats_pager_tabs);
        mRadioGroup.setVisibility(View.VISIBLE);
        mRadioGroup.setOnCheckedChangeListener(this);
        
        for (int i = 0; i < titles.length; i++) {
            RadioButton rb = (RadioButton) LayoutInflater.from(getActivity()).inflate(R.layout.stats_radio_button, null, false);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            int dp4 = (int) Utils.dpToPx(4);
            params.setMargins(dp4, 0, dp4, 0);
            rb.setMinimumWidth((int) Utils.dpToPx(100));
            rb.setGravity(Gravity.CENTER);
            rb.setLayoutParams(params);
            rb.setText(titles[i]);
            mRadioGroup.addView(rb);
            
            if (i == mSelectedButtonIndex)
                rb.setChecked(true);
        }
        
        loadFragmentIndex(mSelectedButtonIndex);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        mSelectedButtonIndex  = group.indexOfChild(group.findViewById(checkedId));
        loadFragmentIndex(mSelectedButtonIndex);
    }
    
    private void loadFragmentIndex(int index) {
        mChildIndex = index;
        if (getChildFragmentManager().findFragmentByTag(CHILD_TAG + ":" + mChildIndex) == null) {
            Fragment fragment = getFragment(index);
            getChildFragmentManager().beginTransaction().replace(R.id.stats_pager_container, fragment, CHILD_TAG + ":" + mChildIndex).commit();
        }
    }
    
    private void initPhoneLayout(View view) {
        mViewPager = (ViewPager) view.findViewById(R.id.stats_pager_viewpager);
        mViewPager.setVisibility(View.VISIBLE);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mTabView.setSelectedTab(position);
            }
        });

        mTabView = (HorizontalTabView) view.findViewById(R.id.stats_pager_tabs);
        mTabView.setVisibility(View.VISIBLE);
        mTabView.setTabListener(this);
        
        mAdapter = getAdapter();
        mViewPager.setAdapter(mAdapter);
        addTabs();
        mTabView.setSelectedTab(0);
    }
    
    private void addTabs() {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            mTabView.addTab(mTabView.newTab().setText(mAdapter.getPageTitle(i)));
        }
    }

    @Override
    public void onTabSelected(Tab tab) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_BUTTON_INDEX, mSelectedButtonIndex);
    }
    
    protected abstract FragmentStatePagerAdapter getAdapter();

    protected abstract String[] getTabTitles();

    protected abstract Fragment getFragment(int position);
        
    @Override
    public void onCursorLoaded(Uri uri, Cursor cursor) {
        if (!cursor.moveToFirst())
            return;
        
        if (cursor.getColumnIndex("date") == -1)
            return;
        
        if (uri.getQueryParameter("timeframe") == null)
            return;
        
        String timeframe = uri.getQueryParameter("timeframe");
        long date = cursor.getLong(cursor.getColumnIndex("date"));
        long currentDate = StatUtils.getCurrentDateMs();
        
        if (timeframe.equals(StatsTimeframe.TODAY.name())) {
            String label1, label2;
            if (date < currentDate) { // old stats
                label1 = StatUtils.msToString(date, "MMM d");
                label2 = StatUtils.msToString(date - ONE_DAY, "MMM d"); // assume the second set of stats is also old, and one day behind
            } else { 
                label1 = StatsTimeframe.TODAY.getLabel();
                label2 = StatsTimeframe.YESTERDAY.getLabel();
            }

            setLabel(0, label1);
            setLabel(1, label2);
        } else if (timeframe.equals(StatsTimeframe.YESTERDAY.name())) {
            
            currentDate -= ONE_DAY; 
            
            String label;
            if (date < currentDate) // old stats
                label = StatUtils.msToString(date, "MMM d");
            else
                label = StatsTimeframe.YESTERDAY.getLabel();
        
            setLabel(1, label);
        }
        
        
    }

    private void setLabel(int position, String label) {
        if (mTabView != null)
            mTabView.setTabText(position, label.toUpperCase(Locale.getDefault()));
        if (mRadioGroup != null) 
            ((RadioButton) mRadioGroup.getChildAt(position)).setText(label);
    }
}
