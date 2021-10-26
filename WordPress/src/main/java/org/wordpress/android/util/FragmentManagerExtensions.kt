package org.wordpress.android.util

import androidx.fragment.app.FragmentManager

fun FragmentManager.clearBackStack() {
    if (backStackEntryCount > 0) {
        popBackStack(getBackStackEntryAt(0).name, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}
