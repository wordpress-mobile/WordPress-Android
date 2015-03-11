/**
 * This file is part of GraphView.
 *
 * GraphView is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GraphView is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GraphView.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 *
 * Copyright Jonas Gehring
 */

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
    private int horizontalBackgroundLabelsColor;
    private IndexDependentColor horizontalLabelsIndexDependentColor;
    private IndexDependentColor horizontalLabelsBackgroundIndexDependentColor;
	private int gridXColor;
	private int gridYColor;
	private float textSize = 30f;
	private int verticalLabelsWidth;
	private int numVerticalLabels;
	private int numHorizontalLabels;
    private int maxColumnWidth; // Max width in PX a column can get on the screen

	public GraphViewStyle() {
		verticalLabelsColor = Color.WHITE;
		horizontalLabelsColor = Color.WHITE;
        horizontalBackgroundLabelsColor = Color.TRANSPARENT;
		gridXColor = Color.DKGRAY;
		gridYColor = Color.DKGRAY;
	}

	public GraphViewStyle(int vLabelsColor, int hLabelsColor, int hLabelsBackgroundColor, int gridXColor, int gridYColor) {
		this.verticalLabelsColor = vLabelsColor;
		this.horizontalLabelsColor = hLabelsColor;
		this.gridXColor = gridXColor;
		this.gridYColor = gridYColor;
        this.horizontalBackgroundLabelsColor = hLabelsBackgroundColor;
	}

	public int getGridXColor() {
		return gridXColor;
	}

	public int getGridYColor() {
		return gridYColor;
	}

    public int getHorizontalLabelsColor(int i) {
        if (horizontalLabelsIndexDependentColor != null) {
            return horizontalLabelsIndexDependentColor.get(i);
        }

        return getHorizontalLabelsColor();
    }

	public int getHorizontalLabelsColor() {
		return horizontalLabelsColor;
	}

    public int getHorizontalLabelsBackgroundColor(int i) {
        if (horizontalLabelsBackgroundIndexDependentColor != null) {
            return horizontalLabelsBackgroundIndexDependentColor.get(i);
        }

        return getHorizontalLabelsBackgroundColor();
    }

    public int getHorizontalLabelsBackgroundColor() {
        return horizontalBackgroundLabelsColor;
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

	public void setGridXColor(int c) {
		gridXColor = c;
	}

	public void setGridYColor(int c) {
		gridYColor = c;
	}

	public void setHorizontalLabelsColor(int c) {
		horizontalLabelsColor = c;
	}


    public int getMaxColumnWidth() {
        return maxColumnWidth;
    }

    public void setMaxColumnWidth(int maxColumnWidth) {
        this.maxColumnWidth = maxColumnWidth;
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

    public IndexDependentColor getHorizontalLabelsIndexDependentColor() {
        return horizontalLabelsIndexDependentColor;
    }

    /**
     * the color depends on the index of the data in the series
     * only possible in BarGraphView
     * @param indexDependentColor
     */
    public void setHorizontalLabelsIndexDependentColor(IndexDependentColor indexDependentColor) {
        this.horizontalLabelsIndexDependentColor = indexDependentColor;
    }

    public IndexDependentColor getHorizontalLabelsBackgroundIndexDependentColor() {
        return horizontalLabelsBackgroundIndexDependentColor;
    }

    public void setHorizontalLabelsBackgroundIndexDependentColor(IndexDependentColor indexDependentColor) {
        this.horizontalLabelsBackgroundIndexDependentColor = indexDependentColor;
    }
}
