package org.wordpress.android.ui.stats;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
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

import java.util.Locale;

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
public abstract class StatsAbsPagedViewFragment extends StatsAbsViewFragment
                                                implements OnCheckedChangeListener,
                                                           StatsCursorInterface {
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
        final TextView titleView = (TextView) view.findViewById(R.id.stats_pager_title);
        titleView.setText(getTitle().toUpperCase(Locale.getDefault()));

        mFragmentContainer = (FrameLayout) view.findViewById(R.id.stats_pager_container);
        mRadioGroup = (RadioGroup) view.findViewById(R.id.stats_pager_tabs);

        int dp8 = (int) Utils.dpToPx(8);
        int dp80 = (int) Utils.dpToPx(80);

        LayoutInflater inflater = LayoutInflater.from(getActivity());

        String[] titles = getTabTitles();
        for (int i = 0; i < titles.length; i++) {
            RadioButton rb = (RadioButton) inflater.inflate(R.layout.stats_radio_button, null, false);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT,
                                                                         RadioGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, dp8, 0);
            rb.setMinimumWidth(dp80);
            rb.setGravity(Gravity.CENTER);
            rb.setLayoutParams(params);
            rb.setText(titles[i]);
            mRadioGroup.addView(rb);

            if (i == mSelectedButtonIndex)
                rb.setChecked(true);
        }

        loadFragmentIndex(mSelectedButtonIndex);

        mRadioGroup.setVisibility(View.VISIBLE);
        mRadioGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // checkedId will be -1 when the selection is cleared
        if (checkedId == -1)
            return;

        int index  = group.indexOfChild(group.findViewById(checkedId));
        if (index == -1)
            return;

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
    public void onCursorLoaded(final Uri uri, Cursor cursor) {
        if (getActivity() == null)
            return;
        if (!cursor.moveToFirst())
            return;

        int colDate = cursor.getColumnIndex("date");
        if (colDate == -1)
            return;

        String timeframe = uri.getQueryParameter("timeframe");
        if (timeframe == null)
            return;

        long date = cursor.getLong(colDate);
        long currentDate = StatUtils.getCurrentDateMs();

        boolean isToday = timeframe.equals(StatsTimeframe.TODAY.name());
        boolean isYesterday = timeframe.equals(StatsTimeframe.YESTERDAY.name());

        final String label0;
        final String label1;
        if (isToday) {
            if (date < currentDate) { // old stats
                label0 = StatUtils.msToString(date, "MMM d");
                label1 = StatUtils.msToString(date - ONE_DAY, "MMM d"); // assume the second set of stats is also old, and one day behind
            } else {
                label0 = StatsTimeframe.TODAY.getLabel();
                label1 = StatsTimeframe.YESTERDAY.getLabel();
            }
        } else if (isYesterday) {
            label0 = null;
            currentDate -= ONE_DAY;
            if (date < currentDate) {// old stats
                label1 = StatUtils.msToString(date, "MMM d");
            } else {
                label1 = StatsTimeframe.YESTERDAY.getLabel();
            }
        } else {
            return;
        }

        if (mRadioGroup == null)
            return;
        final RadioButton radio0 = (RadioButton) mRadioGroup.getChildAt(0);
        final RadioButton radio1 = (RadioButton) mRadioGroup.getChildAt(1);

        if (label0 != null && radio0 != null)
            radio0.setText(label0);
        if (label1 != null && radio1 != null)
            radio1.setText(label1);
    }
}
