package org.wordpress.android.ui.mysite.cards.quickstart

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.ui.mysite.MySiteSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickStartCardSource @Inject constructor(
    private val quickStartRepository: QuickStartRepository
) : MySiteSource<QuickStartUpdate> {
    override fun buildSource(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<QuickStartUpdate> {
        return quickStartRepository.getQuickStartUpdate(coroutineScope, siteLocalId)
    }
}
