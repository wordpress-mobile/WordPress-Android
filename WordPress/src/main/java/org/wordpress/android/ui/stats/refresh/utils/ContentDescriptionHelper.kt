package org.wordpress.android.ui.stats.refresh.utils

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class ContentDescriptionHelper
@Inject constructor(private val resourceProvider: ResourceProvider, private val rtlUtils: RtlUtils) {
    fun buildContentDescription(header: Header, key: String, value: Any): String {
        return buildContentDescription(header.startLabel, key, header.endLabel, value)
    }

    fun buildContentDescription(
        @StringRes keyLabel: Int,
        key: String,
        @StringRes valueLabel: Int,
        value: Any
    ): String {
        return resourceProvider.getString(
            R.string.stats_list_item_description,
            resourceProvider.getString(keyLabel),
            key,
            resourceProvider.getString(valueLabel),
            value
        )
    }

    fun buildContentDescription(header: Header, @StringRes key: Int, value: Any): String {
        return buildContentDescription(header, resourceProvider.getString(key), value)
    }

    fun buildContentDescription(keyLabel: Int, key: Any): String {
        return when (rtlUtils.isRtl) {
            true -> "$key :${resourceProvider.getString(keyLabel)}"
            false -> "${resourceProvider.getString(keyLabel)}: $key"
        }
    }
}
