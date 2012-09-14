Chart and Graph Library for Android
====================================

<h2>What is GraphView</h2>
GraphView is a library for Android to programmatically create flexible and nice-looking diagramms. It is easy to understand, to integrate and to customize it.
At the moment there are two different types:
<ul>
<li>Line Charts</li>
<li>Bar Charts</li>
</ul>

Tested on Android 1.6, 2.2, 2.3 and 3.0 (honeycomb, tablet), 4.0.

<img src="https://github.com/jjoe64/GraphView/raw/master/GVLine.jpg" height="200" />
<img src="https://github.com/jjoe64/GraphView/raw/master/GVBar.png" height="200" />
<img src="http://3.bp.blogspot.com/-BkLSSJSeCt8/TkD4xpeRyGI/AAAAAAAAA6M/sVC_1s_Bf-0/s1600/multi2.png" height="200" />

<h2>Features</h2>

* Two chart types
Line Chart and Bar Chart.
* Draw multiple series of data
Let the diagram show more that one series in a graph. You can set a color and a description for every series.
* Show legend
A legend can be displayed inline the chart. You can set the width and the vertical align (top, middle, bottom).
* Custom labels
The labels for the x- and y-axis are generated automatically. But you can set your own labels, Strings are possible.
* Handle incomplete data
It's possible to give the data in different frequency.
* Viewport
You can limit the viewport so that only a part of the data will be displayed.
* Scrolling
You can scroll with a finger touch move gesture.
* Scaling / Zooming
Since Android 2.3! With two-fingers touch scale gesture (Multi-touch), the viewport can be changed.
* Background (line graph)
Optionally draws a light background under the diagram stroke.
* Manual Y axis limits
* Realtime Graph (Live)

<h2>How to use</h2>
<a href="http://www.jjoe64.com/p/graphview-library.html">View GraphView page http://www.jjoe64.com/p/graphview-library.html</a>

Very simple example:
<pre>
// init example series data
GraphViewSeries exampleSeries = new GraphViewSeries(new GraphViewData[] {
	      new GraphViewData(1, 2.0d)
	      , new GraphViewData(2, 1.5d)
	      , new GraphViewData(3, 2.5d)
	      , new GraphViewData(4, 1.0d)
});

GraphView graphView = new LineGraphView(
      this // context
      , "GraphViewDemo" // heading
);
graphView.addSeries(exampleSeries); // data

LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
layout.addView(graphView);
</pre>

<h2>Important</h2>
To show you how to integrate the library into an existing project see the GraphView-Demos project!
See GraphView-Demos for examples.
<a href="https://github.com/jjoe64/GraphView-Demos">https://github.com/jjoe64/GraphView-Demos<br/>
<a href="http://www.jjoe64.com/p/graphview-library.html">View GraphView page http://www.jjoe64.com/p/graphview-library.html</a>

