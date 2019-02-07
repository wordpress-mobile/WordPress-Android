package org.wordpress.android.ui.stats.refresh.lists.sections.insights

import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode

interface InsightUseCaseFactory {
    fun build(useCaseMode: UseCaseMode): BaseStatsUseCase<*, *>
}
