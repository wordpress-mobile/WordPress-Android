package org.wordpress.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.GridView;

// based on http://stackoverflow.com/questions/8481844/gridview-height-gets-cut

public class ExpandableHeightGridView extends GridView
{

    boolean expanded = false;

    public ExpandableHeightGridView(Context context)
    {
        super(context);
    }

    public ExpandableHeightGridView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ExpandableHeightGridView(Context context, AttributeSet attrs,
            int defStyle)
    {
        super(context, attrs, defStyle);
    }

    public boolean isExpanded()
    {
        return expanded;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        if (isExpanded())
        {
            // Calculate entire height by providing a very large height hint.
            // View.MEASURED_SIZE_MASK represents the largest height possible.
            int expandSpec = MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK,
                    MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, expandSpec);

            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = getMeasuredHeight();
        }
        else
        {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void setExpanded(boolean expanded)
    {
        this.expanded = expanded;
    }
}