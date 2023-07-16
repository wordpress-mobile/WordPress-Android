package org.wordpress.android.util

import androidx.annotation.StringRes
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class StringProvider @Inject constructor(
    private val contextProvider: ContextProvider
) {
    fun getString(@StringRes resId: Int) =
        contextProvider.getContext().getString(resId)
}
