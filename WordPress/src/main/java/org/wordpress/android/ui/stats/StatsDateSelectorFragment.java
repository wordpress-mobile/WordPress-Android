package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.wordpress.android.R;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.TypefaceCache;

public class StatsDateSelectorFragment extends StatsAbstractFragment
        implements RadioGroup.OnCheckedChangeListener {

    private static final String SELECTED_BUTTON_INDEX = "SELECTED_BUTTON_INDEX";
    private int mSelectedButtonIndex = 0;
    private TimeframeChangeListener timeframeChangeListener;
    private RadioGroup mRadioGroup;

    private static final StatsTimeframe[] TIMEFRAMES = new StatsTimeframe[]{
            StatsTimeframe.DAY,
            StatsTimeframe.WEEK,
            StatsTimeframe.MONTH,
            StatsTimeframe.YEAR
    };

    public static final String TAG = StatsDateSelectorFragment.class.getSimpleName();

    public interface TimeframeChangeListener {
        public void onTimeFrameChanged(StatsTimeframe timeframe);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_timeframe_selector_fragment, container, false);
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
        mRadioGroup = (RadioGroup) view.findViewById(R.id.stats_pager_tabs);

        int dp8 = DisplayUtils.dpToPx(view.getContext(), 8);
        int dp80 = DisplayUtils.dpToPx(view.getContext(), 80);

        LayoutInflater inflater = LayoutInflater.from(getActivity());

        String[] titles = StatsTimeframe.toStringArray(TIMEFRAMES);
        for (int i = 0; i < titles.length; i++) {
            RadioButton rb = (RadioButton) inflater.inflate(R.layout.stats_radio_button, null, false);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT);
            rb.setTypeface((TypefaceCache.getTypeface(view.getContext())));

            params.setMargins(0, 0, dp8, 0);
            rb.setMinimumWidth(dp80);
            rb.setGravity(Gravity.CENTER);
            rb.setLayoutParams(params);
            rb.setText(titles[i]);
            mRadioGroup.addView(rb);

            if (i == mSelectedButtonIndex)
                rb.setChecked(true);
        }

        mRadioGroup.setVisibility(View.VISIBLE);
        mRadioGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // checkedId will be -1 when the selection is cleared
        if (checkedId == -1)
            return;

        int index = group.indexOfChild(group.findViewById(checkedId));
        if (index == -1)
            return;

        mSelectedButtonIndex = index;

        if (timeframeChangeListener != null) {
            timeframeChangeListener.onTimeFrameChanged(TIMEFRAMES[index]);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_BUTTON_INDEX, mSelectedButtonIndex);
    }

    @Override
    public String getTitle() {
        return "";
    }

    public void setTimeframeChangeListener(TimeframeChangeListener timeframeChangeListener) {
        this.timeframeChangeListener = timeframeChangeListener;
    }
}
