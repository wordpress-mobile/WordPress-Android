package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import org.wordpress.android.R;

public class WPButtonPreference extends WPPreference {
    public WPButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);

        if (view.findViewById(R.id.button) != null) {
            final WPButtonPreference wpButtonPreference = this;

            Button button = (Button) view.findViewById(R.id.button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getOnPreferenceClickListener().onPreferenceClick(wpButtonPreference);
                }
            });
        }
    }
}
