package org.wordpress.android.ui.jetpack.common.providers

import androidx.annotation.StringRes
import org.wordpress.android.R.string
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.CONTENTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.MEDIA_UPLOADS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.ROOTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.SQLS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.THEMES
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.PLUGINS
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class provides the available item choices for Jetpack Backup Download & Restore
 */
@Singleton
class JetpackAvailableItemsProvider @Inject constructor() {
    fun getAvailableItems(): List<JetpackAvailableItem> {
        return listOf(
                JetpackAvailableItem(
                        THEMES,
                        string.backup_item_themes
                ),
                JetpackAvailableItem(
                        PLUGINS,
                        string.backup_item_plugins
                ),
                JetpackAvailableItem(
                        MEDIA_UPLOADS,
                        string.backup_item_media_uploads
                ),
                JetpackAvailableItem(
                        SQLS,
                        string.backup_item_sqls
                ),
                JetpackAvailableItem(
                        ROOTS,
                        string.backup_item_roots
                ),
                JetpackAvailableItem(
                        CONTENTS,
                        string.backup_item_contents
                )
        )
    }

    data class JetpackAvailableItem(
        val availableItemType: JetpackAvailableItemType,
        @StringRes val labelResId: Int
    )

    enum class JetpackAvailableItemType {
        THEMES,
        PLUGINS,
        MEDIA_UPLOADS,
        SQLS,
        ROOTS,
        CONTENTS
    }
}
