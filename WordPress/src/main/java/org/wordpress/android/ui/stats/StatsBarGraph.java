package org.wordpress.android.ui.stats;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.IndexDependentColor;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A Bar graph depicting the view and visitors.
 * Based on BarGraph from the GraphView library.
 */
class StatsBarGraph extends GraphView {
    private static final int DEFAULT_MAX_Y = 10;

    // Keep tracks of every bar drawn on the graph.
    private final List<List<BarChartRect>> mSeriesRectsDrawedOnScreen = (List<List<BarChartRect>>) new LinkedList();
    private int mBarPositionToHighlight = -1;
    private boolean[] mWeekendDays;

    private final StatsBarGraphAccessibilityHelper mStatsBarGraphAccessibilityHelper;
    private final List<BarChartRect> mVirtualBars = new ArrayList<>();
    private String[] mAccessibleVirtualLabels;

    private final GestureDetectorCompat mDetector;
    private OnGestureListener mGestureListener;

    StatsBarGraph(Context context) {
        super(context, "");

        int width = LayoutParams.MATCH_PARENT;
        int height = getResources().getDimensionPixelSize(R.dimen.stats_barchart_height);
        setLayoutParams(new LayoutParams(width, height));

        setProperties();

        mDetector = new GestureDetectorCompat(getContext(), new MyGestureListener());
        mDetector.setIsLongpressEnabled(false);

        if (isInEditMode()) {
            // Special considerations for edit mode.
            mStatsBarGraphAccessibilityHelper = null;
        } else {
            // Set up accessibility helper class.
            mStatsBarGraphAccessibilityHelper = new StatsBarGraphAccessibilityHelper(this);
            ViewCompat.setAccessibilityDelegate(this, mStatsBarGraphAccessibilityHelper);
        }
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        if (mStatsBarGraphAccessibilityHelper != null && mStatsBarGraphAccessibilityHelper.dispatchHoverEvent(event)) {
            return true;
        }

        return super.dispatchHoverEvent(event);
    }

    public void setGestureListener(OnGestureListener listener) {
        this.mGestureListener = listener;
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            highlightBarAndBroadcastDate();
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            highlightBarAndBroadcastDate();
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    private void highlightBarAndBroadcastDate() {
        highlightBarAndBroadcastDateAt(getTappedBar());
    }

    private void highlightBarAndBroadcastDateAt(int barIndex) {
        if (barIndex >= 0) {
            highlightBar(barIndex);
            if (mGestureListener != null) {
                mGestureListener.onBarTapped(barIndex);
            }
            if (mStatsBarGraphAccessibilityHelper != null) {
                mStatsBarGraphAccessibilityHelper.invalidateVirtualView(barIndex);
            }
        }
    }

    // accessibility is emplemented with StatsBarGraphAccessibilityHelper
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDetector != null) {
            this.mDetector.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    private class HorizontalLabelsColor implements IndexDependentColor {
        public int get(int index) {
            if (mBarPositionToHighlight == index) {
                return getResources().getColor(R.color.orange_jazzy);
            } else {
                return getResources().getColor(R.color.grey_darken_30);
            }
        }
    }

    private void setProperties() {
        GraphViewStyle gStyle = getGraphViewStyle();
        gStyle.setHorizontalLabelsIndexDependentColor(new HorizontalLabelsColor());
        gStyle.setHorizontalLabelsColor(getResources().getColor(R.color.grey_darken_30));
        gStyle.setVerticalLabelsColor(getResources().getColor(R.color.grey_darken_10));
        gStyle.setTextSize(getResources().getDimensionPixelSize(R.dimen.text_sz_extra_small));
        gStyle.setGridXColor(Color.TRANSPARENT);
        gStyle.setGridYColor(getResources().getColor(R.color.grey_lighten_30));
        gStyle.setNumVerticalLabels(3);

        setCustomLabelFormatter(new CustomLabelFormatter() {
            private NumberFormat mNumberFormatter;

            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    return null;
                }
                if (mNumberFormatter == null) {
                    mNumberFormatter = NumberFormat.getNumberInstance();
                    mNumberFormatter.setMaximumFractionDigits(0);
                }
                return mNumberFormatter.format(value);
            }
        });
    }

    @Override
    protected void onBeforeDrawSeries() {
        mSeriesRectsDrawedOnScreen.clear();
    }

    @Override
    public void drawSeries(Canvas canvas, GraphViewDataInterface[] values,
                           float graphwidth, float graphheight, float border, double minX,
                           double minY, double diffX, double diffY, float horstart,
                           GraphViewSeriesStyle style) {
        float colwidth = graphwidth / values.length;
        int maxColumnSize = getGraphViewStyle().getMaxColumnWidth();
        if (maxColumnSize > 0 && colwidth > maxColumnSize) {
            colwidth = maxColumnSize;
        }

        paint.setStrokeWidth(style.thickness);
        paint.setColor(style.color);

        // Bar chart position of this series on the canvas
        List<BarChartRect> barChartRects = new LinkedList<>();

        // draw data
        for (int i = 0; i < values.length; i++) {
            float valY = (float) (values[i].getY() - minY);
            float ratY = (float) (valY / diffY);
            float y = graphheight * ratY;

            // hook for value dependent color
            if (style.getValueDependentColor() != null) {
                paint.setColor(style.getValueDependentColor().get(values[i]));
            }

            float pad = style.padding;

            float left = (i * colwidth) + horstart;
            float top = (border - y) + graphheight;
            float right = left + colwidth;
            float bottom = graphheight + border - 1;

            // Draw the orange selection behind the selected bar
            if (style.outerhighlightColor != 0x00ffffff && mBarPositionToHighlight == i) {
                paint.setColor(style.outerhighlightColor);
                canvas.drawRect(left, 10f, right, bottom, paint);
            }

            // Draw the grey background color on weekend days
            if (style.outerColor != 0x00ffffff
                && mBarPositionToHighlight != i
                && mWeekendDays != null && mWeekendDays[i]) {
                paint.setColor(style.outerColor);
                canvas.drawRect(left, 10f, right, bottom, paint);
            }

            if (mVirtualBars.size() < values.length) {
                mVirtualBars.add(new BarChartRect(left - pad, 10f, right + pad, bottom));
            }

            if ((top - bottom) == 1) {
                // draw a placeholder
                if (mBarPositionToHighlight != i) {
                    paint.setColor(style.color);
                    paint.setAlpha(25);
                    Shader shader =
                            new LinearGradient(left + pad, bottom - 50, left + pad, bottom, Color.WHITE, Color.BLACK,
                                               Shader.TileMode.CLAMP);
                    paint.setShader(shader);
                    canvas.drawRect(left + pad, bottom - 50, right - pad, bottom, paint);
                    paint.setShader(null);
                }
            } else {
                // draw a real bar
                paint.setAlpha(255);
                if (mBarPositionToHighlight == i) {
                    paint.setColor(style.highlightColor);
                } else {
                    paint.setColor(style.color);
                }
                canvas.drawRect(left + pad, top, right - pad, bottom, paint);
            }

            barChartRects.add(new BarChartRect(left + pad, top, right - pad, bottom));
        }
        mSeriesRectsDrawedOnScreen.add(barChartRects);
    }

    private int getTappedBar() {
        float[] lastBarChartTouchedPoint = this.getLastTouchedPointOnCanvasAndReset();
        if (lastBarChartTouchedPoint[0] == 0f && lastBarChartTouchedPoint[1] == 0f) {
            return -1;
        }
        return getTappedBarAt(lastBarChartTouchedPoint[0], lastBarChartTouchedPoint[1]);
    }

    private int getTappedBarAt(float x, float y) {
        if (x == 0f && y == 0f) {
            return -1;
        }
        for (List<BarChartRect> currentSerieChartRects : mSeriesRectsDrawedOnScreen) {
            int i = 0;
            for (BarChartRect barChartRect : currentSerieChartRects) {
                if (barChartRect.isPointInside(x, y)) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    public void setWeekendDays(boolean[] days) {
        mWeekendDays = days;
    }

    public void highlightBar(int barPosition) {
        mBarPositionToHighlight = barPosition;
        this.redrawAll();
    }

    public int getHighlightedBar() {
        return mBarPositionToHighlight;
    }

    public void resetHighlightBar() {
        mBarPositionToHighlight = -1;
    }

    @Override
    protected double getMinY() {
        return 0;
    }

    public void setAccessibleHorizontalLabels(String[] virtualLabels) {
        mAccessibleVirtualLabels = virtualLabels;
    }

    public String getAccessibleHorizontalLabelForBarAt(int index) {
        if (mAccessibleVirtualLabels != null && index < mAccessibleVirtualLabels.length) {
            return mAccessibleVirtualLabels[index];
        }
        AppLog.w(AppLog.T.STATS, "Missing StatsBarGraph accessible label at index " + index);
        return "";
    }

    // Make sure the highest number is always even, so the halfway mark is correctly balanced in the middle of the graph
    // Also make sure to display a default value when there is no activity in the period.
    @Override
    protected double getMaxY() {
        double maxY = super.getMaxY();
        if (maxY == 0) {
            return DEFAULT_MAX_Y;
        }

        return maxY + (maxY % 2);
    }

    /**
     * Private class that is used to hold the local (to the canvas) coordinate on the screen
     * of every single bar in the graph
     */
    private class BarChartRect {
        final float mLeft;
        final float mTop;
        final float mRight;
        final float mBottom;

        BarChartRect(float left, float top, float right, float bottom) {
            this.mLeft = left;
            this.mTop = top;
            this.mRight = right;
            this.mBottom = bottom;
        }

        private Rect toRect() {
            return new Rect(Math.round(mLeft), Math.round(mTop), Math.round(mRight), Math.round(mBottom));
        }

        /**
         * Check if the tap happens on a bar in the graph.
         *
         * @return true if the tap point falls within the bar for the X coordinate, and within the full canvas
         * height for the Y coordinate. This is a fix to make very small bars tappable.
         */
        public boolean isPointInside(float x, float y) {
            return x >= this.mLeft
                   && x <= this.mRight;
        }
    }

    interface OnGestureListener {
        void onBarTapped(int tappedBar);
    }

    private class StatsBarGraphAccessibilityHelper extends ExploreByTouchHelper {
        StatsBarGraphAccessibilityHelper(View parentView) {
            super(parentView);
        }

        @Override protected int getVirtualViewAt(float x, float y) {
            final int index = getTappedBarAt(x, y);
            if (index >= 0) {
                return index;
            }

            return ExploreByTouchHelper.INVALID_ID;
        }

        @Override protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
                final int count = mVirtualBars.size();
                for (int index = 0; index < count; index++) {
                    virtualViewIds.add(index);
            }
        }

        @Override protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfoCompat node) {
            node.setContentDescription(getAccessibleHorizontalLabelForBarAt(virtualViewId));

            node.setSelected(getHighlightedBar() == virtualViewId);

            node.setClickable(mGestureListener != null);
            node.setFocusable(true);

            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            node.setBoundsInParent(mVirtualBars.get(virtualViewId).toRect());
        }


        @Override protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_CLICK:
                    highlightBarAndBroadcastDateAt(virtualViewId);
                    return true;
            }

            return false;
        }
    }
}
