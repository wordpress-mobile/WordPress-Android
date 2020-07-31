package org.wordpress.android.ui.mlp

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData

/**
 * Defines the layout selection listener providing a callback
 * method and access to livedata [LifecycleOwner] and selected data
 */
interface LayoutSelectionListener {
    val lifecycleOwner: LifecycleOwner
    val selectedItemData: LiveData<String?>

    /**
     * Tap on layout event triggered
     * @param layout the tapped layout
     */
    fun layoutTapped(layout: LayoutListItem)
}
