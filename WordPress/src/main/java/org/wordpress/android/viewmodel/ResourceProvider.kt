package org.wordpress.android.viewmodel

import android.graphics.drawable.Drawable
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

    fun getStringArray(id: Int): Array<String> = contextProvider.getContext().resources.getStringArray(id)

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

    /**
     * Formats the string for the given quantity, using the given arguments.
     * We need this because our translation platform doesn't support Android plurals.
     *
     * @param zero The desired string identifier to get when quantity is exactly 0
     * @param one The desired string identifier to get when quantity is exactly 1
     * @param other The desired string identifier to get when quantity is not (0 or 1)
     * @param quantity The number used to get the correct string
     */
    fun getQuantityString(@StringRes zero: Int, @StringRes one: Int, @StringRes other: Int, quantity: Int): String? {
        if (quantity == 0) {
            return contextProvider.getContext().getString(zero)
        }
        return if (quantity == 1) {
            contextProvider.getContext().getString(one)
        } else String.format(contextProvider.getContext().getString(other), quantity)
    }

    fun getDimensionPixelOffset(id: Int): Int {
        return contextProvider.getContext().resources.getDimensionPixelOffset(id)
    }

    fun getDrawable(iconId: Int): Drawable? {
        return ContextCompat.getDrawable(contextProvider.getContext(), iconId)
    }
}
