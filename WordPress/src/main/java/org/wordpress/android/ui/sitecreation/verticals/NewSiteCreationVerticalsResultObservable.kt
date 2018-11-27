package org.wordpress.android.ui.sitecreation.verticals

import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observable used for propagating selected vertical to the parent Activity/ViewModel/Fragment.
 */
@Singleton
class NewSiteCreationVerticalsResultObservable @Inject constructor() {
    val selectedVertical = SingleLiveEvent<String?>()
}
