package org.wordpress.android.ui.stats;

import java.util.Locale;

import android.support.v4.app.FragmentTransaction;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;
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
public abstract class StatsAbsPagedViewFragment extends StatsAbsViewFragment implements OnCheckedChangeListener, StatsCursorInterface {

    private static final int ONE_DAY = 24 * 60 * 60 * 1000;

    private static final String SELECTED_BUTTON_INDEX = "SELECTED_BUTTON_INDEX";
    private int mSelectedButtonIndex = 0;
    
    // the active fragment has the tag CHILD_TAG:<mChildIndex>
    private static final String CHILD_TAG = "CHILD_TAG";

    private RadioGroup mRadioGroup;
    private FrameLayout mFragmentContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_pager_fragment, container, false);
        
        setRetainInstance(true);
        
        initLayout(view);

        restoreState(savedInstanceState);
        
        return view;
    }
    
    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;
        
        mSelectedButtonIndex = savedInstanceState.getInt(SELECTED_BUTTON_INDEX);
    }

    private void initLayout(View view) {
        
        TextView titleView = (TextView) view.findViewById(R.id.stats_pager_title);
        titleView.setText(getTitle().toUpperCase(Locale.getDefault()));
        
        String[] titles = getTabTitles();

        mFragmentContainer = (FrameLayout) view.findViewById(R.id.stats_pager_container);
        
        mRadioGroup = (RadioGroup) view.findViewById(R.id.stats_pager_tabs);
        mRadioGroup.setVisibility(View.VISIBLE);
        mRadioGroup.setOnCheckedChangeListener(this);
        
        for (int i = 0; i < titles.length; i++) {
            RadioButton rb = (RadioButton) LayoutInflater.from(getActivity()).inflate(R.layout.stats_radio_button, null, false);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            int dp8 = (int) Utils.dpToPx(8);
            params.setMargins(0, 0, dp8, 0);
            rb.setMinimumWidth((int) Utils.dpToPx(80));
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
        // checkedId will be -1 when the selection is cleared
        if (checkedId == -1) {
            AppLog.w(AppLog.T.STATS, "checkedId is -1");
            return;
        }

        int index  = group.indexOfChild(group.findViewById(checkedId));
        if (index == -1) {
            AppLog.w(AppLog.T.STATS, "invalid checkedId");
            return;
        }

        mSelectedButtonIndex = index;
        loadFragmentIndex(mSelectedButtonIndex);
    }

    private void loadFragmentIndex(int index) {
        if (index == -1) {
            AppLog.w(AppLog.T.STATS, "invalid fragment index");
            return;
        }

        String childTag = CHILD_TAG + ":" + index;
        if (getChildFragmentManager().findFragmentByTag(childTag) == null) {
            //set minimum height for container, so we don't get a janky fragment transaction
            mFragmentContainer.setMinimumHeight(mFragmentContainer.getHeight());
            Fragment fragment = getFragment(index);
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.stats_fade_in, R.anim.stats_fade_out);
            ft.replace(R.id.stats_pager_container, fragment, childTag);
            ft.commit();
        }
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
        if (mRadioGroup != null) 
            ((RadioButton) mRadioGroup.getChildAt(position)).setText(label);
    }
}
