package org.wordpress.android.userflags.provider

import android.database.Cursor
import android.net.Uri
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.provider.query.QueryContentProvider
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.ui.prefs.AppPrefs.DeletablePrefKey
import org.wordpress.android.ui.prefs.AppPrefs.UndeletablePrefKey
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.publicdata.ClientVerification
import org.wordpress.android.util.signature.SignatureNotFoundException
import javax.inject.Inject

class UserFlagsProvider : QueryContentProvider() {
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var queryResult: QueryResult
    @Inject lateinit var clientVerification: ClientVerification

    private val userFlagsKeysSet: Set<String> = setOf(
            DeletablePrefKey.MAIN_PAGE_INDEX.name,
            DeletablePrefKey.IMAGE_OPTIMIZE_ENABLED.name,
            DeletablePrefKey.IMAGE_OPTIMIZE_WIDTH.name,
            DeletablePrefKey.IMAGE_OPTIMIZE_QUALITY.name,
            DeletablePrefKey.VIDEO_OPTIMIZE_ENABLED.name,
            DeletablePrefKey.VIDEO_OPTIMIZE_WIDTH.name,
            DeletablePrefKey.VIDEO_OPTIMIZE_QUALITY.name,
            DeletablePrefKey.STRIP_IMAGE_LOCATION.name,
            DeletablePrefKey.SUPPORT_EMAIL.name,
            DeletablePrefKey.SUPPORT_NAME.name,
            DeletablePrefKey.GUTENBERG_DEFAULT_FOR_NEW_POSTS.name,
            DeletablePrefKey.USER_IN_GUTENBERG_ROLLOUT_GROUP.name,
            DeletablePrefKey.SHOULD_AUTO_ENABLE_GUTENBERG_FOR_THE_NEW_POSTS.name,
            DeletablePrefKey.SHOULD_AUTO_ENABLE_GUTENBERG_FOR_THE_NEW_POSTS_PHASE_2.name,
            DeletablePrefKey.GUTENBERG_OPT_IN_DIALOG_SHOWN.name,
            DeletablePrefKey.GUTENBERG_FOCAL_POINT_PICKER_TOOLTIP_SHOWN.name,
            DeletablePrefKey.POST_LIST_AUTHOR_FILTER.name,
            DeletablePrefKey.POST_LIST_VIEW_LAYOUT_TYPE.name,
            DeletablePrefKey.AZTEC_EDITOR_DISABLE_HW_ACC_KEYS.name,
            DeletablePrefKey.READER_DISCOVER_WELCOME_BANNER_SHOWN.name,
            DeletablePrefKey.SHOULD_SCHEDULE_CREATE_SITE_NOTIFICATION.name,
            UndeletablePrefKey.THEME_IMAGE_SIZE_WIDTH.name,
            UndeletablePrefKey.BOOKMARKS_SAVED_LOCALLY_DIALOG_SHOWN.name,
            UndeletablePrefKey.IMAGE_OPTIMIZE_PROMO_REQUIRED.name,
            UndeletablePrefKey.SWIPE_TO_NAVIGATE_NOTIFICATIONS.name,
            UndeletablePrefKey.SWIPE_TO_NAVIGATE_READER.name,
            UndeletablePrefKey.IS_MAIN_FAB_TOOLTIP_DISABLED.name,
            UndeletablePrefKey.SHOULD_SHOW_STORIES_INTRO.name,
            UndeletablePrefKey.SHOULD_SHOW_STORAGE_WARNING.name
    )

    private val userFlagsCompositeKeysSet: Set<String> = setOf(
            DeletablePrefKey.SHOULD_SHOW_WEEKLY_ROUNDUP_NOTIFICATION.name
    )

    override fun onCreate(): Boolean {
        return true
    }

    @Suppress("SwallowedException")
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        inject()
        return context?.let {
            try {
                if (clientVerification.canTrust(callingPackage)) {
                    val userFlagsMap = appPrefsWrapper.getAllPrefs()
                            .filter { entry ->
                                userFlagsKeysSet.contains(entry.key)
                                        || userFlagsCompositeKeysSet.firstOrNull { flagKey ->
                                    entry.key.startsWith(flagKey)
                                } != null
                            }
                    queryResult.createCursor(userFlagsMap)
                } else null
            } catch (signatureNotFoundException: SignatureNotFoundException) {
                null
            }
        }
    }

    private fun inject() {
        if (!this::appPrefsWrapper.isInitialized) {
            (context?.applicationContext as WordPress).component().inject(this)
        }
    }
}
