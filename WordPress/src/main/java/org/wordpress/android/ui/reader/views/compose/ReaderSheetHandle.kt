package org.wordpress.android.ui.reader.views.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The drag handle at the top of the Reader's bottom sheets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSheetHandle() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        BottomSheetDefaults.DragHandle()
    }
}
