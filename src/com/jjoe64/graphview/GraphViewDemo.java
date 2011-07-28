package com.jjoe64.graphview;

import android.app.Activity;
import android.os.Bundle;

import com.jjoe64.graphview.GraphView.GraphViewData;

/**
 * GraphViewDemo creates some dummy data to demonstrate the GraphView component.
 *
 * IMPORTANT: For examples take a look at GraphView-Demos (https://github.com/jjoe64/GraphView-Demos)
 *
 */
public class GraphViewDemo extends Activity {
	/**
	 * @param savedInstanceState
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LineGraphView graphView = new LineGraphView(
				this
				, new GraphViewData[] {
						new GraphViewData(1, 2.0d)
						, new GraphViewData(2, 1.5d)
						, new GraphViewData(2.5, 3.0d)
						, new GraphViewData(3, 2.5d)
						, new GraphViewData(4, 1.0d)
						, new GraphViewData(5, 3.0d)
				}
				, "GraphViewDemo"
				, null
				, null
		);
		setContentView(graphView);
	}
}
