package org.wordpress.android.util.extensions

import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.model.ReviewErrorCode
import org.wordpress.android.util.AppLog

fun Task<ReviewInfo>.logException() {
    val errorMessage = "Error fetching ReviewInfo object from Review API to start in-app review process"
    (exception as? ReviewException)?.let {
        @ReviewErrorCode val reviewErrorCode = it.errorCode
        AppLog.e(AppLog.T.UTILS, errorMessage, reviewErrorCode)
    } ?: AppLog.e(AppLog.T.UTILS, "$errorMessage: ${exception?.message}")
}
