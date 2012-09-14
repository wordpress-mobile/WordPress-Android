package com.jjoe64.graphview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.TargetApi;

import com.jjoe64.graphview.GraphView.GraphViewData;

public class GraphViewSeries {
	/**
	 * graph series style: color and thickness
	 */
	static public class GraphViewStyle {
		public int color = 0xff0077cc;
		public int thickness = 3;
		public GraphViewStyle() {
			super();
		}
		public GraphViewStyle(int color, int thickness) {
			super();
			this.color = color;
			this.thickness = thickness;
		}
	}

	final String description;
	final GraphViewStyle style;
	GraphViewData[] values;
	private final List<GraphView> graphViews = new ArrayList<GraphView>();

	public GraphViewSeries(GraphViewData[] values) {
		description = null;
		style = new GraphViewStyle();
		this.values = values;
	}

	public GraphViewSeries(String description, GraphViewStyle style, GraphViewData[] values) {
		super();
		this.description = description;
		if (style == null) {
			style = new GraphViewStyle();
		}
		this.style = style;
		this.values = values;
	}

	/**
	 * this graphview will be redrawn if data changes
	 * @param graphView
	 */
	public void addGraphView(GraphView graphView) {
		this.graphViews.add(graphView);
	}

	/**
	 * add one data to current data
	 * @param value the new data to append
	 * @param scrollToEnd true => graphview will scroll to the end (maxX)
	 */
	@TargetApi(9)
	public void appendData(GraphViewData value, boolean scrollToEnd) {
		GraphViewData[] newValues = Arrays.copyOf(values, values.length+1);
		newValues[values.length] = value;
		values = newValues;
		for (GraphView g : graphViews) {
			if (scrollToEnd) {
				g.scrollToEnd();
			}
		}
	}

	/**
	 * clears the current data and set the new.
	 * redraws the graphview(s)
	 * @param values new data
	 */
	public void resetData(GraphViewData[] values) {
		this.values = values;
		for (GraphView g : graphViews) {
			g.redrawAll();
		}
	}
}
