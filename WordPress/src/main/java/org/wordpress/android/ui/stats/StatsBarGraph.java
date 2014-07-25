package org.wordpress.android.ui.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;

import org.wordpress.android.R;

import java.util.LinkedList;
import java.util.List;

/**
 * A Bar graph depicting the view and visitors.
 * Based on BarGraph from the GraphView library.
 */
class StatsBarGraph extends GraphView {
    // Keep tracks of every bar drawn on the graph.
    private List<List<BarChartRect>> mSeriesRectsDrawedOnScreen = (List<List<BarChartRect>>) new LinkedList();
    private int mBarPositionToHighlight = -1;

    public StatsBarGraph(Context context) {
        super(context, "");

        setProperties();
    }

    private void setProperties() {
        getGraphViewStyle().setHorizontalLabelsColor(Color.BLACK);
        getGraphViewStyle().setVerticalLabelsColor(Color.BLACK);
        getGraphViewStyle().setTextSize(getResources().getDimensionPixelSize(R.dimen.graph_font_size));
        getGraphViewStyle().setGridXColor(Color.TRANSPARENT);
        getGraphViewStyle().setGridYColor(getResources().getColor(R.color.stats_bar_graph_grid));
        getGraphViewStyle().setNumVerticalLabels(6);

        setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    return null;
                }

                if (value < 1000) {
                    return null;
                } else if (value < 1000000) { // thousands
                    return Math.round(value / 1000) + "K";
                } else if (value < 1000000000) { // millions
                    return Math.round(value / 1000000) + "M";
                } else {
                    return null;
                }
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

        paint.setStrokeWidth(style.thickness);
        paint.setColor(style.color);

        // Bar chart position of this series on the canvas
        List<BarChartRect> barChartRects = new LinkedList<BarChartRect>();

        // draw data
        for (int i = 0; i < values.length; i++) {
            float valY = (float) (values[i].getY() - minY);
            float ratY = (float) (valY / diffY);
            float y = graphheight * ratY;

            // hook for value dependent color
            if (style.getValueDependentColor() != null) {
                paint.setColor(style.getValueDependentColor().get(values[i]));
            }

            // Trick to redraw the tapped bar
            if (mBarPositionToHighlight == i) {
                int color;
                if (style.color == getResources().getColor(R.color.stats_bar_graph_views)) {
                    color = getResources().getColor(R.color.stats_views_hover_color);
                } else {
                    color = getResources().getColor(R.color.stats_visitors_hover_color);
                }
                paint.setColor(color);
            } else {
                paint.setColor(style.color);
            }

            float pad = style.padding;

            float left = (i * colwidth) + horstart;
            float top = (border - y) + graphheight;
            float right = left + colwidth;
            float bottom = graphheight + border - 1;

            canvas.drawRect(left + pad, top, right - pad, bottom, paint);
            barChartRects.add(new BarChartRect(left + pad, top, right - pad, bottom));
        }
        mSeriesRectsDrawedOnScreen.add(barChartRects);
    }

    public int getTappedBar() {
        float[] lastBarChartTouchedPoint = this.getLastTouchedPointOnCanvasAndReset();
        if (lastBarChartTouchedPoint[0] == 0f && lastBarChartTouchedPoint[1] == 0f) {
            return -1;
        }
        for (List<BarChartRect> currentSerieChartRects : mSeriesRectsDrawedOnScreen) {
            int i = 0;
            for (BarChartRect barChartRect : currentSerieChartRects) {
                if (barChartRect.isPointInside(lastBarChartTouchedPoint[0], lastBarChartTouchedPoint[1])) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    public void highlightAndDismissBar(int barPosition) {
        mBarPositionToHighlight = barPosition;
        if (mBarPositionToHighlight == -1) {
            return;
        }
        this.redrawAll();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBarPositionToHighlight = -1;
                redrawAll();
            }
        }, 500);
    }

    public void highlightBar(int barPosition) {
        mBarPositionToHighlight = barPosition;
        this.redrawAll();
    }

    @Override
    protected double getMinY() {
        return 0;
    }

    @Override
    protected double getMaxY() {
        double maxY = super.getMaxY();

        final int divideBy;
        if (maxY < 100) {
            divideBy = 10;
        } else if (maxY < 1000) {
            divideBy = 100;
        } else if (maxY < 10000) {
            divideBy = 1000;
        } else if (maxY < 100000) {
            divideBy = 10000;
        } else if (maxY < 1000000) {
            divideBy = 100000;
        } else {
            divideBy = 1000000;
        }

        maxY = Math.rint((maxY / divideBy) + 1) * divideBy;
        return maxY;
    }


    /**
     * Private class that is used to hold the local (to the canvas) coordinate on the screen
     * of every single bar in the graph
     */
    private class BarChartRect {
        float mLeft, mTop, mRight, mBottom;

        BarChartRect(float left, float top, float right, float bottom) {
            this.mLeft = left;
            this.mTop = top;
            this.mRight = right;
            this.mBottom = bottom;
        }

        /**
         * Check if the tap happens on a bar in the graph.
         *
         * @return true if the tap point falls within the bar for the X coordinate, and within the full canvas
         * height for the Y coordinate. This is a fix to make very small bars tappable.
         */
        public boolean isPointInside(float x, float y) {
            if (x >= this.mLeft
                    && x <= this.mRight
                    && (this.mBottom - this.mTop) > 1f
                    ) {
                return true;
            }
            return false;
        }
    }
}
