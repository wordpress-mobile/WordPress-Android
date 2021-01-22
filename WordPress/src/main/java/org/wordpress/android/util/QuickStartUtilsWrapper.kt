package org.wordpress.android.util

import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject

class QuickStartUtilsWrapper
@Inject constructor(
    private val quickStartStore: QuickStartStore,
    private val selectedSiteRepository: SelectedSiteRepository
) {
    fun isQuickStartInProgress(): Boolean {
        return selectedSiteRepository.getSelectedSite()
                ?.let { QuickStartUtils.isQuickStartInProgress(quickStartStore, it.id) } ?: false
    }
}
