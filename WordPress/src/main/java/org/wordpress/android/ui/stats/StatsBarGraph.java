package org.wordpress.android.ui.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;

import java.util.LinkedList;
import java.util.List;

/**
 * A Bar graph depicting the view and visitors.
 * Based on BarGraph from the GraphView library.
 */
class StatsBarGraph extends GraphView {
    private final List<BarChartRect> barChartRects = new LinkedList<BarChartRect>();

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
                if(isValueX)
                    return null;

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
	public void drawSeries(Canvas canvas, GraphViewDataInterface[] values,
			float graphwidth, float graphheight, float border, double minX,
			double minY, double diffX, double diffY, float horstart,
			GraphViewSeriesStyle style) {
		float colwidth = graphwidth / values.length;

		paint.setStrokeWidth(style.thickness);
		paint.setColor(style.color);

        barChartRects.clear();

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

			canvas.drawRect(left + pad, top, right - pad, bottom, paint);
            barChartRects.add(new BarChartRect(left + pad, top, right - pad, bottom));
		}
        canvas.drawRect(0, 0, 10, 10, paint);
	}

    private class BarChartRect {
        float left, top, right, bottom;

        BarChartRect(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public boolean isPointInside(float x, float y) {
            int leftOffset = getLeft();
            int top = getTop();
            if (x >= this.left + leftOffset && x <= this.right &&
                    y>= this.bottom && y>= this.top)
            {
                return true;
            }
            return false;
        }
    }

    public boolean isTapOnBar(float x, float y) {
        for (BarChartRect barChartRect : barChartRects) {
            if (barChartRect.isPointInside(x,y)) {
                return true;
            }
        }
        return false;
    }

	@Override
	protected double getMinY() {
		return 0;
	}

	@Override
	protected double getMaxY() {
		double maxY = super.getMaxY();

		final int divideBy;
		if (maxY < 100)
			divideBy = 10;
		else if (maxY < 1000)
			divideBy = 100;
		else if (maxY < 10000)
			divideBy = 1000;
		else if (maxY < 100000)
			divideBy = 10000;
		else if (maxY < 1000000)
			divideBy = 100000;
		else
			divideBy = 1000000;

		maxY = Math.rint((maxY / divideBy) + 1) * divideBy;
		return maxY;

	}
}
