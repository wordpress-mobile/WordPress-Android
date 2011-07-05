package com.jjoe64.graphview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.view.View;

/**
 * GraphView creates a scaled line or bar graph with x and y axis labels.
 * @author originally: Arno den Hond
 *
 */
public class GraphView extends View {
	static final private class GraphViewConfig {
		static final float BORDER = 20;
		static final float VERTICAL_LABEL_WIDTH = 100;
		static final float HORIZONTAL_LABEL_HEIGHT = 80;
	}
	static public class GraphViewData {
		double valueX;
		double valueY;
		public GraphViewData(double valueX, double valueY) {
			super();
			this.valueX = valueX;
			this.valueY = valueY;
		}
	}

	private final Paint paint;
	private final Paint paintBackground;
	private GraphViewData[] values;
	private String[] horlabels;
	private String[] verlabels;
	private String title;

	/**
	 *
	 * @param context
	 * @param values must be sorted by valueX ASC
	 * @param title [optional]
	 * @param horlabels [optional] if null, labels were generated automatically
	 * @param verlabels [optional] if null, labels were generated automatically
	 */
	public GraphView(Context context, GraphViewData[] values, String title, String[] horlabels, String[] verlabels) {
		super(context);
		if (values == null)
			values = new GraphViewData[0];
		else
			this.values = values;
		if (title == null)
			title = "";
		else
			this.title = title;

		this.horlabels = horlabels;
		this.verlabels = verlabels;

		paint = new Paint();
		paintBackground = new Paint();
		paintBackground.setARGB(255, 10, 20, 30);
		paintBackground.setStrokeCap(Paint.Cap.ROUND);
		paintBackground.setStrokeWidth(4);
	}

	private String[] generateHorlabels(float graphwidth) {
		int numLabels = (int) (graphwidth/GraphViewConfig.VERTICAL_LABEL_WIDTH);
		String[] labels = new String[numLabels+1];
		double min = getMinX();
		double max = getMaxX();
		for (int i=0; i<=numLabels; i++) {
			labels[i] = String.valueOf(min + ((max-min)*i/numLabels));
		}
		return labels;
	}

	private String[] generateVerlabels(float graphheight) {
		int numLabels = (int) (graphheight/GraphViewConfig.HORIZONTAL_LABEL_HEIGHT);
		String[] labels = new String[numLabels+1];
		double min = getMinY();
		double max = getMaxY();
		for (int i=0; i<=numLabels; i++) {
			labels[i] = String.valueOf(min + ((max-min)*i/numLabels));
		}
		return labels;
	}

	private double getMaxX() {
		// values must be sorted by x, so the last value has the largest X value
		return values[values.length-1].valueX;
	}

	private double getMaxY() {
		double largest = Integer.MIN_VALUE;
		for (int i = 0; i < values.length; i++)
			if (values[i].valueY > largest)
				largest = values[i].valueY;
		return largest;
	}

	private double getMinX() {
		// values must be sorted by x, so the first value has the smallest X value
		return values[0].valueX;
	}

	private double getMinY() {
		double smallest = Integer.MAX_VALUE;
		for (int i = 0; i < values.length; i++)
			if (values[i].valueY < smallest)
				smallest = values[i].valueY;
		return smallest;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// normal
		paint.setStrokeWidth(0);

		float border = GraphViewConfig.BORDER;
		float horstart = border * 2;
		float height = getHeight();
		float width = getWidth() - 1;
		double maxY = getMaxY();
		double minY = getMinY();
		double diffY = maxY - minY;
		double maxX = getMaxX();
		double minX = getMinX();
		double diffX = maxX - minX;
		float graphheight = height - (2 * border);
		float graphwidth = width - (2 * border);

		if (horlabels == null) {
			horlabels = generateHorlabels(graphwidth);
		}
		if (verlabels == null) {
			verlabels = generateVerlabels(graphheight);
		}

		// vertical labels + lines
		paint.setTextAlign(Align.LEFT);
		int vers = verlabels.length - 1;
		for (int i = 0; i < verlabels.length; i++) {
			paint.setColor(Color.DKGRAY);
			float y = ((graphheight / vers) * i) + border;
			canvas.drawLine(horstart, y, width, y, paint);
			paint.setColor(Color.WHITE);
			canvas.drawText(verlabels[i], 0, y, paint);
		}

		// horizontal labels + lines
		int hors = horlabels.length - 1;
		for (int i = 0; i < horlabels.length; i++) {
			paint.setColor(Color.DKGRAY);
			float x = ((graphwidth / hors) * i) + horstart;
			canvas.drawLine(x, height - border, x, border, paint);
			paint.setTextAlign(Align.CENTER);
			if (i==horlabels.length-1)
				paint.setTextAlign(Align.RIGHT);
			if (i==0)
				paint.setTextAlign(Align.LEFT);
			paint.setColor(Color.WHITE);
			canvas.drawText(horlabels[i], x, height - 4, paint);
		}

		paint.setTextAlign(Align.CENTER);
		canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);

		if (maxY != minY) {
			// blue version
			paint.setARGB(255, 0, 119, 204);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStrokeWidth(3);

			// draw data
			double lastY = 0;
			double lastX = 0;
			for (int i = 0; i < values.length; i++) {
				double valY = values[i].valueY - minY;
				double ratY = valY / diffY;
				double y = graphheight * ratY;

				double valX = values[i].valueX - minX;
				double ratX = valX / diffX;
				double x = graphwidth * ratX;

				if (i > 0) {
					float startX = (float) lastX + (horstart + 1);
					float startY = (float) (border - lastY) + graphheight;
					float endX = (float) x + (horstart + 1);
					float endY = (float) (border - y) + graphheight;

					canvas.drawLine(startX, startY, endX, endY, paint);
				}
				lastY = y;
				lastX = x;
			}
		}
	}
}
