package org.wordpress.android.util

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import org.wordpress.android.R
import org.wordpress.android.util.ToastUtils.Duration

fun requestEmailValidation(context: Context, email: String?) {
    val message = if (!TextUtils.isEmpty(email)) {
        context.getString(R.string.my_site_verify_your_email, email)
    } else {
        context.getString(R.string.my_site_verify_your_email_without_email)
    }

    ToastUtils.showToast(
            context,
            message,
            Duration.LONG,
            Gravity.BOTTOM,
            0,
            context.resources.getDimensionPixelOffset(R.dimen.smart_toast_offset_y)
    )
}
