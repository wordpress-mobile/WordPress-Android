package org.wordpress.android.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * EditText that uses OpenSans as default font type
 */
public class OpenSansEditText extends EditText {
    public OpenSansEditText(Context context) {
        super(context);
        setOpenSans();
    }

    public OpenSansEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOpenSans();
    }

    public OpenSansEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOpenSans();
    }

    private void setOpenSans() {
        Context context = getContext();
        if (context != null) {
            Typeface face = Typeface.createFromAsset(context.getAssets(), "fonts/OpenSans-Regular.ttf");
            setTypeface(face);
        }
    }
}
