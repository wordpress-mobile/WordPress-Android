package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.TypefaceCache;

public class WPSwitchPreference extends SwitchPreference {
    public WPSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);

        TextView titleView = (TextView) view.findViewById(android.R.id.title);
        if (titleView != null) {
            Resources res = getContext().getResources();
            Typeface typeface = TypefaceCache.getTypeface(getContext(),
                    TypefaceCache.FAMILY_OPEN_SANS,
                    Typeface.NORMAL,
                    TypefaceCache.VARIATION_NORMAL);

            titleView.setTypeface(typeface);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.text_sz_large));
            titleView.setTextColor(res.getColor(isEnabled() ? R.color.grey_dark : R.color.grey_lighten_10));
        }
    }
}
