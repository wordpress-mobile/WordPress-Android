package com.jjoe64.graphview;

import android.graphics.Color;

/**
 * Styles for the GraphView
 * Important: Use GraphViewSeries.GraphViewSeriesStyle for series-specify styling
 *
 */
public class GraphViewStyle {
	private int verticalLabelsColor;
	private int horizontalLabelsColor;
	private int gridColor;
	private float textSize = 30f;
	private int verticalLabelsWidth;
	private int numVerticalLabels;
	private int numHorizontalLabels;

	public GraphViewStyle() {
		verticalLabelsColor = Color.WHITE;
		horizontalLabelsColor = Color.WHITE;
		gridColor = Color.DKGRAY;
	}

	public GraphViewStyle(int vLabelsColor, int hLabelsColor, int gridColor) {
		this.verticalLabelsColor = vLabelsColor;
		this.horizontalLabelsColor = hLabelsColor;
		this.gridColor = gridColor;
	}

	public int getGridColor() {
		return gridColor;
	}

	public int getHorizontalLabelsColor() {
		return horizontalLabelsColor;
	}

	public int getNumHorizontalLabels() {
		return numHorizontalLabels;
	}

	public int getNumVerticalLabels() {
		return numVerticalLabels;
	}

	public float getTextSize() {
		return textSize;
	}

	public int getVerticalLabelsColor() {
		return verticalLabelsColor;
	}

	public int getVerticalLabelsWidth() {
		return verticalLabelsWidth;
	}

	public void setGridColor(int c) {
		gridColor = c;
	}

	public void setHorizontalLabelsColor(int c) {
		horizontalLabelsColor = c;
	}

	/**
	 * @param numHorizontalLabels 0 = auto
	 */
	public void setNumHorizontalLabels(int numHorizontalLabels) {
		this.numHorizontalLabels = numHorizontalLabels;
	}

	/**
	 * @param numVerticalLabels 0 = auto
	 */
	public void setNumVerticalLabels(int numVerticalLabels) {
		this.numVerticalLabels = numVerticalLabels;
	}

	public void setTextSize(float textSize) {
		this.textSize = textSize;
	}

	public void setVerticalLabelsColor(int c) {
		verticalLabelsColor = c;
	}

	/**
	 * @param verticalLabelsWidth 0 = auto
	 */
	public void setVerticalLabelsWidth(int verticalLabelsWidth) {
		this.verticalLabelsWidth = verticalLabelsWidth;
	}
}
