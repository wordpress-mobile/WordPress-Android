Chart and Graph Library for Android
====================================

<h2>What is GraphView</h2>
GraphView is a small library for Android to programmatically create flexible and nice-looking line diagramms. It is easy to understand, to integrate and to customize it.

GraphView was originally created by arnodenhond, but I did a complete remake and so now I can say it is "my" library ;-)

Tested on Android 1.6, 2.2, 2.3 and 3.0 (honeycomb, tablet).

<img src="https://github.com/jjoe64/GraphView/raw/master/GVLine.jpg" />

<h2>Features</h2>

* Custom labels
The labels for the x- and y-axis are generated automatically. But you can set your only labels, Strings are possible.
* Background
Optionally draws a light background under the diagramm stroke.
* Handle incomplete data
It's possible to give the data in different frequency.
* Viewport
You can limit the viewport so that only a part of the data will be displayed.
* Scrolling
You can scroll with a finger touch move gesture.
* Scaling / Zooming
Since Android 2.3! With two-fingers touch scale gesture, the viewport can be changed.

<h2>How to use</h2>
<a href="http://www.jjoe64.com/2011/07/chart-and-graph-library-for-android.html">http://www.jjoe64.com/2011/07/chart-and-graph-library-for-android.html</a>

// graph with dynamically genereated horizontal and vertical labels
GraphView graphView = new GraphView(
  this // context
  , new GraphViewData[] {
    new GraphViewData(1, 2.0d)
    , new GraphViewData(2, 1.5d)
    , new GraphViewData(2.5, 3.0d) // another frequency
    , new GraphViewData(3, 2.5d)
    , new GraphViewData(4, 1.0d)
    , new GraphViewData(5, 3.0d)
  } // data
  , "GraphViewDemo" // heading
  , null // dynamic labels
  , null // dynamic labels
);
LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);
layout.addView(graphView);

<h2>Important</h2>
To show you how to integrate the library into an existing project see the GraphView-Demos project!
See GraphView-Demos for examples.
<a href="https://github.com/jjoe64/GraphView-Demos">https://github.com/jjoe64/GraphView-Demos</a>
<a href="http://www.jjoe64.com/2011/07/chart-and-graph-library-for-android.html">http://www.jjoe64.com/2011/07/chart-and-graph-library-for-android.html</a>

