package org.wordpress.android.util

import android.content.Context
import androidx.annotation.StringRes
import dagger.Reusable
import javax.inject.Inject

@Reusable
class ToastUtilsWrapper @Inject constructor() {
    fun showToast(context: Context, @StringRes messageRes: Int) =
        ToastUtils.showToast(context, messageRes)
}
