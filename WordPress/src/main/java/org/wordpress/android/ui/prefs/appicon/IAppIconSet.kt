package org.wordpress.android.ui.prefs.appicon

/**
 * This interface represents the set of App Icons available for each specific flavor.
 *
 * The interface should be implemented by each flavor in the appropriate source-set and injected using DI with the name
 * [AppIconSet]. The interface only exists to make sure the class in each flavor implements the same members.
 */
interface IAppIconSet {
    val appIcons: List<AppIcon>
}
