package org.wordpress.android.ui.prefs

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable wrapper around AppPrefs.
 *
 * AppPrefs interface is consisted of static methods, which make the client code difficult to test/mock. Main purpose of
 * this wrapper is to make testing easier.
 *
 */
@Singleton
class AppPrefsWrapper @Inject constructor() {
    var newsCardDismissedVersion: Int
        get() = AppPrefs.getNewsCardDismissedVersion()
        set(version) = AppPrefs.setNewsCardDismissedVersion(version)

    var newsCardShownVersion: Int
        get() = AppPrefs.getNewsCardShownVersion()
        set(version) = AppPrefs.setNewsCardShownVersion(version)

    var avatarVersion: Int
        get() = AppPrefs.getAvatarVersion()
        set(version) = AppPrefs.setAvatarVersion(version)
}
