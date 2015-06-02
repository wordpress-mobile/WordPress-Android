package com.jjoe64.graphview;

/**
 * you can change the color of an element depending on the index in the series.
 * takes only effect in BarGraphView
 */
public interface IndexDependentColor {
	public int get(int index);
}
