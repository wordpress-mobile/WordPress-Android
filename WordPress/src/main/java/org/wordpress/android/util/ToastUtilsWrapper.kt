package org.wordpress.android.util

import androidx.annotation.StringRes
import dagger.Reusable
import org.wordpress.android.WordPress
import javax.inject.Inject

@Reusable
class ToastUtilsWrapper @Inject constructor() {
    fun showToast(@StringRes messageRes: Int) =
        ToastUtils.showToast(WordPress.getContext(), messageRes)
}
