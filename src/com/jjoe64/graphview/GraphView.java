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

	public static boolean BAR = true;
	public static boolean LINE = false;

	private final Paint paint;
	private final Paint paintBackground;
	private float[] values;
	private String[] horlabels;
	private String[] verlabels;
	private String title;
	private final boolean type;

	public GraphView(Context context, float[] values, String title, String[] horlabels, String[] verlabels, boolean type) {
		super(context);
		if (values == null)
			values = new float[0];
		else
			this.values = values;
		if (title == null)
			title = "";
		else
			this.title = title;
		if (horlabels == null)
			this.horlabels = new String[0];
		else
			this.horlabels = horlabels;
		if (verlabels == null)
			this.verlabels = new String[0];
		else
			this.verlabels = verlabels;
		this.type = type;
		paint = new Paint();
		paintBackground = new Paint();
		paintBackground.setARGB(255, 10, 20, 30);
		paintBackground.setStrokeCap(Paint.Cap.ROUND);
		paintBackground.setStrokeWidth(4);
	}

	private float getMax() {
		float largest = Integer.MIN_VALUE;
		for (int i = 0; i < values.length; i++)
			if (values[i] > largest)
				largest = values[i];
		return largest;
	}

	private float getMin() {
		float smallest = Integer.MAX_VALUE;
		for (int i = 0; i < values.length; i++)
			if (values[i] < smallest)
				smallest = values[i];
		return smallest;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// normal
		paint.setStrokeWidth(0);

		float border = 20;
		float horstart = border * 2;
		float height = getHeight();
		float width = getWidth() - 1;
		float max = getMax();
		float min = getMin();
		float diff = max - min;
		float graphheight = height - (2 * border);
		float graphwidth = width - (2 * border);

		paint.setTextAlign(Align.LEFT);
		int vers = verlabels.length - 1;
		for (int i = 0; i < verlabels.length; i++) {
			paint.setColor(Color.DKGRAY);
			float y = ((graphheight / vers) * i) + border;
			canvas.drawLine(horstart, y, width, y, paint);
			paint.setColor(Color.WHITE);
			canvas.drawText(verlabels[i], 0, y, paint);
		}
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

		if (max != min) {
			paint.setColor(Color.LTGRAY);
			if (type == BAR) {
				float datalength = values.length;
				float colwidth = (width - (2 * border)) / datalength;
				for (int i = 0; i < values.length; i++) {
					float val = values[i] - min;
					float rat = val / diff;
					float h = graphheight * rat;
					canvas.drawRect((i * colwidth) + horstart, (border - h) + graphheight, ((i * colwidth) + horstart) + (colwidth - 1), height - (border - 1), paint);
				}
			} else {
				// blue version
				paint.setARGB(255, 0, 119, 204);
				paint.setStrokeCap(Paint.Cap.ROUND);
				paint.setStrokeWidth(3);

				float datalength = values.length;
				float colwidth = (width - (2 * border)) / datalength;
				float halfcol = colwidth / 2;

				// first draw background
				float lasth = 0;
				for (int i = 0; i < values.length; i++) {
					float val = values[i] - min;
					float rat = val / diff;
					float h = graphheight * rat;

					canvas.drawLine((i * colwidth) + horstart, (border - lasth) + graphheight +4, (i * colwidth) + horstart, graphheight+border, paintBackground);
					if (i > 0) {
						float startX = ((i - 1) * colwidth) + (horstart + 1) + halfcol;
						float startY = (border - lasth) + graphheight;

						float endX = (i * colwidth) + (horstart + 1) + halfcol;
						float endY = (border - h) + graphheight;

						for (int xi=1; xi<8; xi++) {
							canvas.drawLine(startX+(endX-startX)/8*xi, startY+(endY-startY)/8*xi +4, endX+(endX-startX)/8*xi, graphheight+border, paintBackground);
						}
					}
					lasth = h;
				}
				// draw data
				lasth = 0;
				for (int i = 0; i < values.length; i++) {
					float val = values[i] - min;
					float rat = val / diff;
					float h = graphheight * rat;

					if (i > 0) {
						float startX = ((i - 1) * colwidth) + (horstart + 1) + halfcol;
						float startY = (border - lasth) + graphheight;
						float endX = (i * colwidth) + (horstart + 1) + halfcol;
						float endY = (border - h) + graphheight;

						canvas.drawLine(startX, startY, endX, endY, paint);
					}
					lasth = h;
				}
			}
		}
	}
}
