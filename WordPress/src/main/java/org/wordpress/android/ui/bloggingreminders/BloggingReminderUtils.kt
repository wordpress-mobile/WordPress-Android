package org.wordpress.android.ui.bloggingreminders

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.observeEvent

object BloggingReminderUtils {
    @JvmStatic
    fun observeBottomSheet(
        isBottomSheetShowing: LiveData<Event<Boolean>>,
        lifecycleOwner: LifecycleOwner,
        tag: String,
        getSupportFragmentManager: () -> FragmentManager?
    ) {
        isBottomSheetShowing.observeEvent(lifecycleOwner,
            { isShowing: Boolean ->
                val fm: FragmentManager = getSupportFragmentManager() ?: return@observeEvent
                var bottomSheet = fm.findFragmentByTag(tag) as BloggingReminderBottomSheetFragment?
                if (isShowing && bottomSheet == null) {
                    bottomSheet = BloggingReminderBottomSheetFragment()
                    bottomSheet.show(
                        fm,
                        tag
                    )
                } else if (!isShowing && bottomSheet != null) {
                    bottomSheet.dismiss()
                }
            })
    }

    @JvmStatic
    fun observeTimePicker(
        isTimePickerShowing: LiveData<Event<Boolean>>,
        lifecycleOwner: LifecycleOwner,
        tag: String,
        getSupportFragmentManager: () -> FragmentManager?
    ) {
        isTimePickerShowing.observeEvent(lifecycleOwner,
            { isShowing: Boolean ->
                val fm: FragmentManager = getSupportFragmentManager() ?: return@observeEvent
                var timePicker = fm.findFragmentByTag(tag) as BloggingReminderTimePicker?

                if (isShowing && timePicker == null) {
                    timePicker = BloggingReminderTimePicker.newInstance()
                    timePicker.show(fm, tag)
                } else if (!isShowing && timePicker != null) {
                    timePicker.dismiss()
                }
            }
        )
    }
}
