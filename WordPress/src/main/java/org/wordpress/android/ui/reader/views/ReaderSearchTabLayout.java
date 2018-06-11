package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.lang.reflect.Field;

public class ReaderSearchTabLayout extends TabLayout {
    public ReaderSearchTabLayout(Context context) {
        super(context);
        init();
    }

    public ReaderSearchTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ReaderSearchTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int indicatorColor = ContextCompat.getColor(getContext(), R.color.tab_indicator);
        int textColor = ContextCompat.getColor(getContext(), R.color.blue_light);
        int selectedTextColor = ContextCompat.getColor(getContext(), R.color.white);

        setSelectedTabIndicatorColor(indicatorColor);
        setTabMode(MODE_FIXED);
        setTabGravity(GRAVITY_FILL);
        setTabTextColors(textColor, selectedTextColor);

        addTab(newTab().setText(R.string.posts));
        addTab(newTab().setText(R.string.sites));

        // set the tab max width to zero - this is the only way to get the tabs to fill the space in landscape
        try {
            Field field = TabLayout.class.getDeclaredField("mRequestedTabMaxWidth");
            field.setAccessible(true);
            field.set(this, 0);
        } catch (Exception e) {
            AppLog.e(T.READER, e);
        }
    }
}
