package org.wordpress.android.ui.accounts

import androidx.fragment.app.Fragment

sealed class LoginNavigationEvents {
    data class SlideInFragment(val fragment: Fragment, val shouldAddToBackStack: Boolean, val tag: String) :
            LoginNavigationEvents()
}
