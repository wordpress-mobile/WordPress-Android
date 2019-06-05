package org.wordpress.android.viewmodel.helpers

import android.content.Context
import androidx.annotation.StringRes
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration

class ToastMessageHolder(
    @StringRes val messageRes: Int,
    val duration: Duration
) {
    fun show(context: Context) {
        ToastUtils.showToast(context, messageRes, duration)
    }
}
