package org.wordpress.android.widgets;

import android.content.Context;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import android.util.AttributeSet;

public class AutoCompleteEmptyTextView extends AppCompatAutoCompleteTextView {
    public AutoCompleteEmptyTextView(Context context) {
        super(context);
    }

    public AutoCompleteEmptyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoCompleteEmptyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }
}
