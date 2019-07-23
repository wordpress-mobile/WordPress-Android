package org.wordpress.android.ui.stats.refresh.utils

import org.wordpress.android.R
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class ContentDescriptionHelper
@Inject constructor(private val resourceProvider: ResourceProvider) {
    fun buildContentDescription(keyLabel: Int, key: String, valueLabel: Int, value: Int): String {
        return resourceProvider.getString(
                R.string.stats_list_item_description,
                resourceProvider.getString(keyLabel),
                key,
                resourceProvider.getString(valueLabel),
                value
        )
    }

    fun buildContentDescription(keyLabel: Int, key: String, valueLabel: Int, value: String): String {
        return resourceProvider.getString(
                R.string.stats_list_item_description,
                resourceProvider.getString(keyLabel),
                key,
                resourceProvider.getString(valueLabel),
                value
        )
    }

    fun buildContentDescription(keyLabel: Int, key: Int, valueLabel: Int, value: Int): String {
        return resourceProvider.getString(
                R.string.stats_list_item_description,
                resourceProvider.getString(keyLabel),
                resourceProvider.getString(key),
                resourceProvider.getString(valueLabel),
                value
        )
    }

    fun buildContentDescription(keyLabel: Int, key: String): String {
        return resourceProvider.getString(
                R.string.stats_list_item_short_description,
                resourceProvider.getString(keyLabel),
                key
        )
    }

    fun buildContentDescription(keyLabel: Int, key: Long): String {
        return resourceProvider.getString(
                R.string.stats_list_item_short_description,
                resourceProvider.getString(keyLabel),
                key
        )
    }
}
