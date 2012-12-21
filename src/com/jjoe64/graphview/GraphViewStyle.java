package com.jjoe64.graphview;

import android.graphics.Color;

/**
 * Styles for the GraphView
 * Important: Use GraphViewSeries.GraphViewSeriesStyle for series-specify styling
 *
 */
public class GraphViewStyle {
	private int vLabelsColor;
	private int hLabelsColor;
	private int gridColor;

	public GraphViewStyle() {
		vLabelsColor = Color.WHITE;
		hLabelsColor = Color.WHITE;
		gridColor = Color.DKGRAY;
	}

	public GraphViewStyle(int vLabelsColor, int hLabelsColor, int gridColor) {
		this.vLabelsColor = vLabelsColor;
		this.hLabelsColor = hLabelsColor;
		this.gridColor = gridColor;
	}

	public int getVerticalLabelsColor() {
		return vLabelsColor;
	}

	public int getHorizontalLabelsColor() {
		return hLabelsColor;
	}

	public int getGridColor() {
		return gridColor;
	}

	public void setVerticalLabelsColor(int c) {
		vLabelsColor = c;
	}

	public void setHorizontalLabelsColor(int c) {
		hLabelsColor = c;
	}

	public void setGridColor(int c) {
		gridColor = c;
	}
}
