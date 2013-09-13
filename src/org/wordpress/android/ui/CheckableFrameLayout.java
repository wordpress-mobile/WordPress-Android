package org.wordpress.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.FrameLayout;

import org.wordpress.android.R;

public class CheckableFrameLayout extends FrameLayout implements Checkable {

    private boolean mIsChecked;
    private OnCheckedChangeListener mOnCheckedChangeListener;
    
    public interface OnCheckedChangeListener {

        public void onCheckedChanged(CheckableFrameLayout view, boolean isChecked);

    }
    
    public CheckableFrameLayout(Context context) {
        super(context);
    }
    
    public CheckableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public CheckableFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        CheckBox checkbox = (CheckBox) findViewById(R.id.media_grid_item_checkstate);
        if (checkbox != null)
            checkbox.setChecked(checked);
        
        if (mIsChecked != checked) {
            mIsChecked = checked;
            refreshDrawableState();
            if(mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged((CheckableFrameLayout) this.findViewById(R.id.media_grid_frame_layout), checked);
            }
        }
    }

    @Override
    public void toggle() {
        setChecked(!mIsChecked);
    }
    
    public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckChangeListener) {
        mOnCheckedChangeListener = onCheckChangeListener;
    }

}
