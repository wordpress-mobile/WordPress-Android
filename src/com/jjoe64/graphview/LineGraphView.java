package com.jjoe64.graphview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;

/**
 * Line Graph View. This draws a line chart.
 * @author jjoe64 - jonas gehring - http://www.jjoe64.com
 *
 * Copyright (C) 2011 Jonas Gehring
 * Licensed under the GNU Lesser General Public License (LGPL)
 * http://www.gnu.org/licenses/lgpl.html
 */
public class LineGraphView extends GraphView {
	private final Paint paintBackground;
	private boolean drawBackground;

	public LineGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		paintBackground = new Paint();
		paintBackground.setARGB(255, 20, 40, 60);
		paintBackground.setStrokeWidth(4);
	}
	
	public LineGraphView(Context context, String title) {
		super(context, title);

		paintBackground = new Paint();
		paintBackground.setARGB(255, 20, 40, 60);
		paintBackground.setStrokeWidth(4);
	}

	@Override
	public void drawSeries(Canvas canvas, GraphViewData[] values, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY, float horstart, GraphViewSeriesStyle style) {
		// draw background
		double lastEndY = 0;
		double lastEndX = 0;
		if (drawBackground) {
			float startY = graphheight + border;
			for (int i = 0; i < values.length; i++) {
				double valY = values[i].valueY - minY;
				double ratY = valY / diffY;
				double y = graphheight * ratY;

				double valX = values[i].valueX - minX;
				double ratX = valX / diffX;
				double x = graphwidth * ratX;

				float endX = (float) x + (horstart + 1);
				float endY = (float) (border - y) + graphheight +2;

				if (i > 0) {
					// fill space between last and current point
					double numSpace = ((endX - lastEndX) / 3f) +1;
					for (int xi=0; xi<numSpace; xi++) {
						float spaceX = (float) (lastEndX + ((endX-lastEndX)*xi/(numSpace-1)));
						float spaceY = (float) (lastEndY + ((endY-lastEndY)*xi/(numSpace-1)));

						// start => bottom edge
						float startX = spaceX;

						// do not draw over the left edge
						if (startX-horstart > 1) {
							canvas.drawLine(startX, startY, spaceX, spaceY, paintBackground);
						}
					}
				}

				lastEndY = endY;
				lastEndX = endX;
			}
		}

		// draw data
		paint.setStrokeWidth(style.thickness);
		paint.setColor(style.color);

		lastEndY = 0;
		lastEndX = 0;
		for (int i = 0; i < values.length; i++) {
			double valY = values[i].valueY - minY;
			double ratY = valY / diffY;
			double y = graphheight * ratY;

			double valX = values[i].valueX - minX;
			double ratX = valX / diffX;
			double x = graphwidth * ratX;

			if (i > 0) {
				float startX = (float) lastEndX + (horstart + 1);
				float startY = (float) (border - lastEndY) + graphheight;
				float endX = (float) x + (horstart + 1);
				float endY = (float) (border - y) + graphheight;

				canvas.drawLine(startX, startY, endX, endY, paint);
			}
			lastEndY = y;
			lastEndX = x;
		}
	}

	public boolean getDrawBackground() {
		return drawBackground;
	}

	/**
	 * @param drawBackground true for a light blue background under the graph line
	 */
	public void setDrawBackground(boolean drawBackground) {
		this.drawBackground = drawBackground;
	}
}
