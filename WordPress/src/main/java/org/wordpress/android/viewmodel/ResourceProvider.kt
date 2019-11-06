package org.wordpress.android.viewmodel

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import javax.inject.Inject

class ResourceProvider @Inject constructor(private val contextProvider: ContextProvider) {
    fun getString(@StringRes resourceId: Int): String {
        return contextProvider.getContext().getString(resourceId)
    }

    fun getString(@StringRes resourceId: Int, vararg formatArgs: Any): String {
        return contextProvider.getContext().getString(resourceId, *formatArgs)
    }

    fun getColor(@ColorRes resourceId: Int): Int {
        return ContextCompat.getColor(contextProvider.getContext(), resourceId)
    }

    fun getDimensionPixelSize(@DimenRes dimen: Int): Int {
        val resources = contextProvider.getContext().resources
        return resources.getDimensionPixelSize(dimen)
    }

    fun getDimension(@DimenRes dimen: Int): Float {
        val resources = contextProvider.getContext().resources
        return resources.getDimension(dimen)
    }
}
