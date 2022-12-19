package org.wordpress.android.ui.compose.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

fun Modifier.disableUserScroll() = nestedScroll(
        connection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource) = available.copy(x = 0f)
            override suspend fun onPreFling(available: Velocity) = available.copy(x = 0f)
        }
)
