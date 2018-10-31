package org.wordpress.android.viewmodel.helpers

import android.support.annotation.StringRes
import org.wordpress.android.util.ToastUtils.Duration

class ToastMessageHolder(
    @StringRes val messageRes: Int,
    val duration: Duration
)
