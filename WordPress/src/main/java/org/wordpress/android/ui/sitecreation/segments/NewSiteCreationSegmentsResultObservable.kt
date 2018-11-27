package org.wordpress.android.ui.sitecreation.segments

import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observable used for propagating selected segment to the parent Activity/ViewModel/Fragment.
 */
@Singleton
class NewSiteCreationSegmentsResultObservable @Inject constructor() {
    val selectedSegment = SingleLiveEvent<Long>()
}
