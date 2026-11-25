package org.wordpress.android.ui.reader.views.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The widest a Reader list row gets, so lines stay readable on tablets and in landscape */
val ReaderReadableWidth = 680.dp

/**
 * Centres the row and caps it at [ReaderReadableWidth]. Applied to each lazy item rather than the list, so the
 * whole width still scrolls.
 */
fun Modifier.readableWidth(): Modifier = this
    .fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = ReaderReadableWidth)
    .fillMaxWidth()
