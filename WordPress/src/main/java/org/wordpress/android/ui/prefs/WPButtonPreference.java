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
        setLayoutResource(R.layout.button_preference);
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);

        Button button = (Button) view.findViewById(R.id.button);
    }

    @Override
    public int getLayoutResource() {
        return R.layout.button_preference;
    }
}
