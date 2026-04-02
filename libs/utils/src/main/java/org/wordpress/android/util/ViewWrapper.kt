package org.wordpress.android.util

import android.view.View

/**
 * This class is necessary because standard View doesn't work in unit tests (it's always null)
 */
class ViewWrapper(val view: View)
