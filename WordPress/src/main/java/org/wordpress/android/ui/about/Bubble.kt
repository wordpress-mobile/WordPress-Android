package org.wordpress.android.ui.about

/**
 * This is basic class that holds the values for a specific [BubbleView].
 * The 2D world updates these values after each simulation step and the view updates its values accordingly.
 */
data class Bubble(
    val viewId: Int,
    val viewSize: Int,
    val app: AutomatticApp,
    var viewX: Float,
    var viewY: Float
)
