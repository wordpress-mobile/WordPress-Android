package org.wordpress.android.ui.compose.utils

import android.app.Activity
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch

// Extension for Activity
fun Activity.showAsBottomSheet(content: @Composable BottomSheetScope.() -> Unit) {
    val viewGroup = this.findViewById(android.R.id.content) as ViewGroup
    addContentToView(viewGroup, content)
}

// Extension for Fragment
@Suppress("unused")
fun Fragment.showAsBottomSheet(content: @Composable BottomSheetScope.() -> Unit) {
    requireActivity().showAsBottomSheet(content)
}

// Helper method
private fun addContentToView(
    viewGroup: ViewGroup,
    content: @Composable BottomSheetScope.() -> Unit
) {
    viewGroup.addView(
        ComposeView(viewGroup.context).apply {
            setContent {
                BottomSheetWrapper(viewGroup, this, content)
            }
        }
    )
}

interface BottomSheetScope {
    fun hideBottomSheet()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheetWrapper(
    parent: ViewGroup,
    composeView: ComposeView,
    content: @Composable BottomSheetScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    var isSheetOpened by remember { mutableStateOf(false) }

    val bottomSheetScope = object : BottomSheetScope {
        override fun hideBottomSheet() {
            coroutineScope.launch {
                modalBottomSheetState.hide()
            }
        }
    }

    ModalBottomSheetLayout(
        sheetShape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
        sheetState = modalBottomSheetState,
        sheetContent = {
            bottomSheetScope.content()
        },
    ) {}

    BackHandler {
        bottomSheetScope.hideBottomSheet()
    }

    // Take action based on hidden state
    LaunchedEffect(modalBottomSheetState.currentValue) {
        when (modalBottomSheetState.currentValue) {
            ModalBottomSheetValue.Hidden -> {
                when {
                    isSheetOpened -> parent.removeView(composeView)
                    else -> {
                        isSheetOpened = true
                        modalBottomSheetState.show()
                    }
                }
            }

            else -> {
                // Do nothing
            }
        }
    }
}
