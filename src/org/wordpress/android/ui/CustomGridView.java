
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
import android.widget.ListAdapter;

public class CustomGridView extends GridView implements AdapterView.OnItemLongClickListener, OnTouchListener {

    private int lastX;
    private int lastY;
    private View activeView;
    private int activePos;
    private ArrayList<Integer> newPositions;
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


        int x = (int) event.getX(); 
        int y = (int) event.getY();
        
        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                if (activeView == null)
                    return super.onTouchEvent(event);
                
                int delta_x = x - lastX;
                int delta_y = y - lastY;
                int l = (int) (activeView.getLeft() + delta_x);
                int t = (int) (activeView.getTop() + delta_y);
                int r = l + activeView.getWidth();
                int b = t + activeView.getHeight();
                activeView.layout(l, t, r, b);

                int gap = getGap(x, y);
                animateGap(gap);
                
                
                int[] pos = getLocationOnScreen();
                int bottom = pos[1] + getHeight();
                
                if (y >= 0.66 * bottom && y > lastY)
                    startScrollDown();
                else if (y <= 0.33 * bottom && y < lastY)
                    startScrollUp();
                else
                    stopScroll();

                lastX = x;
                lastY = y;
                break;
            case MotionEvent.ACTION_UP:
                stopScroll();
                gap = getGap(x, y);
                animateActiveViewToGap(gap);
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

            if (getScrollY() + getHeight() + 3 <= bottom) {
                smoothScrollBy(3, 50);
            }
            handler.postDelayed(this, 50);
        }
    };
    
    private Runnable scrollUpRunner = new Runnable() {
        
        @Override
        public void run() {
            if (getScrollY() - 3 >= 0)
                smoothScrollBy(-3, 50);
            handler.postDelayed(this, 50);
        }
    };
    
    private int getGap(int x, int y) {
        if (y < 0)
            return 0;
        
        if (y > mHeight)
            return Integer.MAX_VALUE;
        
        y +=  getScrollY();
        for (int i = 0; i < coords.size(); i++) {
            if (coords.get(i).contains(x, y))
                return i;
        }
        return 0;
    }
    
    protected void animateActiveViewToGap(int gap) {
        if (activeView == null)
            return;

        Point newXY = getCoorFromIndex(gap);
        Point newOffset = new Point(newXY.x - activeView.getLeft(), newXY.y - activeView.getTop());

        TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, 0,
                                                              Animation.ABSOLUTE, newOffset.x,
                                                              Animation.ABSOLUTE, 0,
                                                              Animation.ABSOLUTE, newOffset.y);
        translate.setDuration(150);
        translate.setFillEnabled(true);
        translate.setFillAfter(true);
        activeView.clearAnimation();
        activeView.startAnimation(translate);

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

    private Point getCoorFromIndex(int index) {
        int[] pos = getLocationOnScreen();
        if (index < 0 || coords.size() == 0) {
            return new Point(pos[0], pos[1]);
        } else if (index >= coords.size()) {
            Rect r = coords.get(coords.size() - 1);
            return new Point(r.left, r.top);
        }

        Rect rect = coords.get(index);
        return new Point(rect.left, rect.top);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        coords.clear();
        
        int cols = getNumColumns();
        
        ListAdapter adapter = getAdapter();
        int count = adapter.getCount();
        
        int left = 0;
        int top = 0;
        int width = 0;
        int height = 0;
        
        // assume visible children have same width and height
        View firstChild = getChildAt(0);
        int baseW = firstChild.getWidth();
        int baseH = firstChild.getHeight();
        
        for (int i = 0; i < count; i++) {
            
            for (int j = 0; j < cols; j++) {
            
                View view = adapter.getView(i, null, this);
                if (view.getVisibility() != View.GONE) {
                    width = baseW;
                    height = baseH;
                    Rect rect = new Rect(left, top, left + width, top + height);
                    coords.add(rect);
                } else {
                    width = 0;
                    height = 0;
                }
                
                
                left += width;
            }
            
            left = 0;
            top += height;

        }

        if (coords.size() > 0)
            mHeight = coords.get(coords.size() - 1).bottom;
        else
            mHeight = 0;
    }
    
}
