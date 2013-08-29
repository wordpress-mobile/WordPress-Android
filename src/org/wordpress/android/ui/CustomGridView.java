
package org.wordpress.android.ui;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.GridView;

public class CustomGridView extends GridView implements AdapterView.OnItemLongClickListener, OnTouchListener {

    private int lastX;
    private int lastY;
    private View activeView;
    private int activePos;
    private ArrayList<Integer> newPositions;
    private int activePosPrevX;
    private int activePosPrevY;
    private ArrayList<Rect> coords; 
    private Handler handler;
    private int mHeight;

    public CustomGridView(Context context) {
        super(context);
        init();
    }

    public CustomGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        newPositions = new ArrayList<Integer>();
        coords = new ArrayList<Rect>();
        handler = new Handler();
        
        setOnItemLongClickListener(this);
        setOnTouchListener(this);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        activeView = view;
        activePos = position;
        activePosPrevX = (int) view.getLeft();
        activePosPrevY = (int) view.getTop();
        newPositions.set(activePos, -1);
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        newPositions.clear();
        for (int i = 0; i < getAdapter().getCount(); i++) {
            newPositions.add(i, i);
        }
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        
        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                lastX = (int) event.getX();
                lastY = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (activeView == null)
                    return super.onTouchEvent(event);
                
                int x = (int) event.getX(); 
                int y = (int) event.getY();
                
                int delta_x = x - lastX;
                int delta_y = y - lastY;
                int l = (int) (activeView.getLeft() + delta_x);
                int t = (int) (activeView.getTop() + delta_y);
                int r = l + activeView.getWidth();
                int b = t + activeView.getHeight();
                activeView.layout(l, t, r, b);

                int gap = getGap(x, y);
                animateGap(gap);
                
                lastX = (int) event.getX();
                lastY = (int) event.getY();
                
                int[] pos = getLocationOnScreen();
                int bottom = pos[1] + getHeight();
                
                if (y >= 0.66 * bottom)
                    startScrollDown();
                else if (y <= 0.33 * bottom)
                    startScrollUp();
                else
                    stopScroll();
                
                break;
            case MotionEvent.ACTION_UP:
                stopScroll();
                activeView = null;
                activePos = -1;
                break;
        }
        return false;
    }
    
    private int[] getLocationOnScreen() {
        int[] pos = new int[] {0,0};
        getLocationOnScreen(pos);
        return pos;
    }

    private void startScrollDown() {
        handler.removeCallbacks(scrollUpRunner);
        handler.post(scrollDownRunner);
    }

    private void startScrollUp() {
        handler.removeCallbacks(scrollDownRunner);
        handler.post(scrollUpRunner);
    }
    
    private void stopScroll() {
        handler.removeCallbacks(scrollUpRunner);
        handler.removeCallbacks(scrollDownRunner);
    }

    private Runnable scrollDownRunner = new Runnable() {
        
        @Override
        public void run() {
            int[] pos = new int[]{0,0};
            getLocationInWindow(pos);
            int bottom = mHeight;

            if (getScrollY() + getHeight() + 3 <= bottom)
                scrollBy(0, 3);
            else
                scrollTo(0, bottom);
            handler.postDelayed(this, 50);
        }
    };
    
    private Runnable scrollUpRunner = new Runnable() {
        
        @Override
        public void run() {
            int[] pos = getLocationOnScreen();
            int top = pos[1];
            
            if (getScrollY() - 3 >= top)
                scrollBy(0, -3);
            else
                scrollTo(0, top);
            handler.postDelayed(this, 50);
        }
    };
    
    
    private int getGap(int x, int y) {
        for (int i = 0; i < coords.size(); i++) {
            if (coords.get(i).contains(x, y))
                return i;
        }
        return -1;
    }
    
    protected void animateGap(int target) {
        for (int i = 0; i < getChildCount(); i++) {
            
            if (i == activePos)
                continue;

            View v = getChildAt(i);
            
            int newPos = i;
            if (activePos < target && i >= activePos && i <= target)
                newPos--;
            else if (target < activePos && i >= target && i < activePos)
                newPos++;
            
            
            //animate
            int oldPos = i;
            if (newPositions.get(i) != -1)
                oldPos = newPositions.get(i);
            if (oldPos == newPos)
                continue;

            
            Point oldXY = getCoorFromIndex(oldPos);
            Point newXY = getCoorFromIndex(newPos);
            Point oldOffset = new Point(oldXY.x - v.getLeft(), oldXY.y - v.getTop());
            Point newOffset = new Point(newXY.x - v.getLeft(), newXY.y - v.getTop());

            TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, oldOffset.x,
                                                                  Animation.ABSOLUTE, newOffset.x,
                                                                  Animation.ABSOLUTE, oldOffset.y,
                                                                  Animation.ABSOLUTE, newOffset.y);
            translate.setDuration(150);
            translate.setFillEnabled(true);
            translate.setFillAfter(true);
            v.clearAnimation();
            v.startAnimation(translate);
            
            newPositions.set(i, newPos);
        }
    }

    private Point getCoorFromIndex(int oldPos) {
        int[] pos = getLocationOnScreen();
        if (oldPos < 0)
            return new Point(pos[0], pos[1]);
        else if (oldPos >= coords.size())
            return new Point(pos[0], pos[1] + getHeight());

        Rect rect = coords.get(oldPos);
        return new Point(rect.left, rect.top);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        coords.clear();
        mHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            Rect rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
            coords.add(rect);
            mHeight += rect.bottom - rect.top;
        }
    }
}
