package org.wordpress.android.ui.mysite

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState

interface MySiteSource<T : PartialState> {
    fun buildSource(siteId: Int): Flow<T?>
    interface SiteIndependentSource<T : PartialState> : MySiteSource<T> {
        fun buildSource(): Flow<T?>
        override fun buildSource(siteId: Int): Flow<T?> = buildSource()
    }
}
