package org.wordpress.android;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Popup window, shows action list as icon and text like the one in Gallery3D app. 
 * 
 * @author Lorensius. W. T
 */
public class QuickAction extends PopupWindows {
    private View mRootView;
    private ImageView mArrowUp;
    private ImageView mArrowDown;
    private LayoutInflater inflater;
    private ViewGroup mTrack;
    private ScrollView mScroller;
    private OnActionItemClickListener mListener;
    
    protected static final int ANIM_GROW_FROM_LEFT = 1;
    protected static final int ANIM_GROW_FROM_RIGHT = 2;
    protected static final int ANIM_GROW_FROM_CENTER = 3;
    protected static final int ANIM_REFLECT = 4;
    public static final int ANIM_AUTO = 5;
    
    private int mChildPos;
    private int animStyle;
    
    /**
     * Constructor.
     * 
     * @param context Context
     */
    public QuickAction(Context context) {
        super(context);
        
        inflater        = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        setRootViewId(R.layout.popup);
       
        animStyle       = ANIM_AUTO;
        mChildPos       = 0;
    }

    /**
     * Set root view.
     * 
     * @param id Layout resource id
     */
    public void setRootViewId(int id) {
        mRootView   = (ViewGroup) inflater.inflate(id, null);
        mTrack      = (ViewGroup) mRootView.findViewById(R.id.tracks);

        mArrowDown  = (ImageView) mRootView.findViewById(R.id.arrow_down);
        mArrowUp    = (ImageView) mRootView.findViewById(R.id.arrow_up);

        mScroller   = (ScrollView) mRootView.findViewById(R.id.scroller);
        
        setContentView(mRootView);
    }
    
    /**
     * Set animation style
     * 
     * @param animStyle animation style, default is set to ANIM_AUTO
     */
    public void setAnimStyle(int animStyle) {
        this.animStyle = animStyle;
    }
    
    /**
     * Set listener for action item clicked.
     * 
     * @param listener Listener
     */
    public void setOnActionItemClickListener(OnActionItemClickListener listener) {
        mListener = listener;
    }
    
    /**
     * Add action item
     * 
     * @param action  {@link ActionItem}
     */
    public void addActionItem(ActionItem action) {
        
        String title    = action.getTitle();
        Drawable icon   = action.getIcon();
        
        View container  = (View) inflater.inflate(R.layout.action_item, null);
        
        ImageView img   = (ImageView) container.findViewById(R.id.iv_icon);
        TextView text   = (TextView) container.findViewById(R.id.tv_title);
        
        if (icon != null) 
            img.setImageDrawable(icon);
        else
            img.setVisibility(View.GONE);
        
        if (title != null)
            text.setText(title);
        else
            text.setVisibility(View.GONE);
        
        final int pos =  mChildPos;
        
        container.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) mListener.onItemClick(pos);
                    
                dismiss();
            }
        });
        
        container.setFocusable(true);
        container.setClickable(true);
             
        mTrack.addView(container, mChildPos);
        
        mChildPos++;
    }
    
    /**
     * Show popup window. Popup is automatically positioned, on top or bottom of anchor view.
     * 
     */
    public void show (View anchor) {
        preShow();
        
        int xPos, yPos;
        
        int[] location      = new int[2];
    
        anchor.getLocationOnScreen(location);

        Rect anchorRect     = new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1] 
                            + anchor.getHeight());

        mRootView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mRootView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    
        int rootHeight      = mRootView.getMeasuredHeight();
        int rootWidth       = mRootView.getMeasuredWidth();
        
        int screenWidth     = mWindowManager.getDefaultDisplay().getWidth();
        int screenHeight    = mWindowManager.getDefaultDisplay().getHeight();
        
        //automatically get X coord of popup (top left)
        if ((anchorRect.left + rootWidth) > screenWidth) {
            xPos = anchorRect.left - (rootWidth-anchor.getWidth());
        } else {
            if (anchor.getWidth() > rootWidth) {
                xPos = anchorRect.centerX() - (rootWidth/2);
            } else {
                xPos = anchorRect.left;
            }
        }
        
        int dyTop           = anchorRect.top;
        int dyBottom        = screenHeight - anchorRect.bottom;

        boolean onTop       = (dyTop > dyBottom) ? true : false;

        if (onTop) {
            if (rootHeight > dyTop) {
                yPos            = 15;
                LayoutParams l  = mScroller.getLayoutParams();
                l.height        = dyTop - anchor.getHeight();
            } else {
                yPos = anchorRect.top - rootHeight;
            }
        } else {
            yPos = anchorRect.bottom;
            
            if (rootHeight > dyBottom) { 
                LayoutParams l  = mScroller.getLayoutParams();
                l.height        = dyBottom;
            }
        }
        
        showArrow(((onTop) ? R.id.arrow_down : R.id.arrow_up), anchorRect.centerX()-xPos);
        
        setAnimationStyle(screenWidth, anchorRect.centerX(), onTop);
        
        mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
    }
    
    /**
     * Set animation style
     * 
     * @param screenWidth screen width
     * @param requestedX distance from left edge
     * @param onTop flag to indicate where the popup should be displayed. Set TRUE if displayed on top of anchor view
     *        and vice versa
     */
    private void setAnimationStyle(int screenWidth, int requestedX, boolean onTop) {
        int arrowPos = requestedX - mArrowUp.getMeasuredWidth()/2;

        switch (animStyle) {
        case ANIM_GROW_FROM_LEFT:
            mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left : R.style.Animations_PopDownMenu_Left);
            break;
                    
        case ANIM_GROW_FROM_RIGHT:
            mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right : R.style.Animations_PopDownMenu_Right);
            break;
                    
        case ANIM_GROW_FROM_CENTER:
            mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center : R.style.Animations_PopDownMenu_Center);
        break;
            
        case ANIM_REFLECT:
            mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Reflect : R.style.Animations_PopDownMenu_Reflect);
        break;
        
        case ANIM_AUTO:
            if (arrowPos <= screenWidth/4) {
                mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left : R.style.Animations_PopDownMenu_Left);
            } else if (arrowPos > screenWidth/4 && arrowPos < 3 * (screenWidth/4)) {
                mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center : R.style.Animations_PopDownMenu_Center);
            } else {
                mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right : R.style.Animations_PopDownMenu_Right);
            }
                    
            break;
        }
    }
    
    /**
     * Show arrow
     * 
     * @param whichArrow arrow type resource id
     * @param requestedX distance from left screen
     */
    private void showArrow(int whichArrow, int requestedX) {
        final View showArrow = (whichArrow == R.id.arrow_up) ? mArrowUp : mArrowDown;
        final View hideArrow = (whichArrow == R.id.arrow_up) ? mArrowDown : mArrowUp;

        final int arrowWidth = mArrowUp.getMeasuredWidth();

        showArrow.setVisibility(View.VISIBLE);
        
        ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams)showArrow.getLayoutParams();
       
        param.leftMargin = requestedX - arrowWidth / 2;
        
        hideArrow.setVisibility(View.INVISIBLE);
    }
    
    /**
     * Listener for item click
     *
     */
    public interface OnActionItemClickListener {
        public abstract void onItemClick(int pos);
    }
}