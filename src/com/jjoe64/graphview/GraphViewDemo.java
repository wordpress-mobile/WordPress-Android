package com.jjoe64.graphview;

import android.app.Activity;
import android.os.Bundle;

import com.jjoe64.graphview.GraphView.GraphViewData;

/**
 * GraphViewDemo creates some dummy data to demonstrate the GraphView component.
 * @author originally: Arno den Hond
 *
 */
public class GraphViewDemo extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String[] verlabels = new String[] { "great", "ok", "bad" };
		String[] horlabels = new String[] { "today", "tomorrow", "next week", "next month" };
		GraphView graphView = new GraphView(
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
				, horlabels
				, verlabels
		);
		setContentView(graphView);
	}
}
