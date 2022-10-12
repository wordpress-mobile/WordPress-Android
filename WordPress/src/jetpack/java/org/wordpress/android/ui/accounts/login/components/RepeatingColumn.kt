package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints

/** This composable accepts a child composable, and repeats that composable `repeat` times (default 3). It places the
 * resulting column at a position offset by `position`, in the negative direction on the vertical axis. The repeat
 * parameter is useful to ensure that content still fills the screen when offset by the entire height of the repeated
 * child composable.
 *
 * @param position the position to offset the content - a value greater than or equal to 0 and less than 1 expressed in
 * terms of the height of the child composable
 * @param repeat the number of times to repeat the child composable
 */
@Composable
fun RepeatingColumn(
    position: Float,
    repeat: Int = 3,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
            content = {
                Column(modifier.clearAndSetSemantics {}) {
                    repeat(repeat) {
                        content()
                    }
                }
            }
    ) { measurables, constraints ->
        val placeables = measurables.map {
            it.measure(constraints.copy(maxHeight = Constraints.Infinity))
        }
        val totalHeight = placeables.sumOf { it.height }
        val offsetY = (position * totalHeight / repeat).toInt()
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { it.placeRelative(0, -offsetY) }
        }
    }
}
