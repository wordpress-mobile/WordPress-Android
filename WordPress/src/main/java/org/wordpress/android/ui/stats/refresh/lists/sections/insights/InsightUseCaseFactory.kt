package org.wordpress.android.ui.stats.refresh.lists.sections.insights

import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase

interface InsightUseCaseFactory {
    fun build(isViewAllMode: Boolean): BaseStatsUseCase<*, *>
}
