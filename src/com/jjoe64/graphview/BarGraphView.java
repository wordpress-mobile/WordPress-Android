package com.jjoe64.graphview;

import android.content.Context;
import android.graphics.Canvas;

/**
 * Draws a Bar Chart
 * @author Muhammad Shahab Hameed
 */
public class BarGraphView extends GraphView {
	public BarGraphView(Context context, GraphViewData[] values, String title,
			String[] horlabels, String[] verlabels) {
		super(context, values, title, horlabels, verlabels);
	}

	@Override
	public void drawData(Canvas canvas, float graphwidth, float graphheight,
			float border, double minX, double minY, double diffX, double diffY,
			float horstart) {
		GraphViewData[] values = _values();

		float colwidth = (graphwidth - (2 * border)) / values.length;

		// draw data
		for (int i = 0; i < values.length; i++) {
			float valY = (float) (values[i].valueY - minY);
			float ratY = (float) (valY / diffY);
			float y = graphheight * ratY;
			canvas.drawRect((i * colwidth) + horstart, (border - y) + graphheight, ((i * colwidth) + horstart) + (colwidth - 1), graphheight + border - 1, paint);
		}
	}
}
