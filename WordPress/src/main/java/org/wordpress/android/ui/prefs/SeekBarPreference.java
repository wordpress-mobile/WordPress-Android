package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import org.wordpress.android.R;

/**
 * Smooth scrolling SeekBar that can be configured to snap to equally positioned discrete points
 * (referred to as Progress).
 */

public class SeekBarPreference extends SummaryPreference
        implements SeekBar.OnSeekBarChangeListener {
    private SeekBar mSeekBar;
    private int mMaxValue;

    public SeekBarPreference(Context context) {
        super(context);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mMaxValue = 100;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);

        for (int i = 0; i < array.getIndexCount(); ++i) {
            int index = array.getIndex(i);
            if (index == R.styleable.SeekBarPreference_maxValue) {
                mMaxValue = array.getInt(index, 100);
            }
        }

        array.recycle();
    }

    @Override
    public View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);

        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = li.inflate(R.layout.seekbar_pref, parent, false);
        int seekPosition = mSeekBar == null ? 0 : getProgress();

        if (view != null) {
            mSeekBar = (SeekBar) view.findViewById(R.id.seekbar_pref_bar);
            if (mSeekBar != null) {
                mSeekBar.setOnSeekBarChangeListener(this);
            }
        }

        updateProgress(seekPosition);

        return view;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        updateProgress(getProgress());
    }

    public int getProgress() {
        if (mSeekBar == null) return -1;

        return Math.round((float) mSeekBar.getProgress() / 100.f * mMaxValue);
    }

    public void setProgress(int progress) {
        updateProgress(Math.min(mMaxValue, progress));
    }

    private void updateProgress(int progress) {
        if (mSeekBar == null) return;

        mSeekBar.setProgress((int) ((float) progress / mMaxValue * 100));

        callChangeListener(progress);
    }
}
