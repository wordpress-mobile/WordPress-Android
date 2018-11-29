package org.wordpress.android.ui.stats.refresh.lists.sections.dwmy

import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase

interface UseCaseFactory {
    fun build(granularity: StatsGranularity): BaseStatsUseCase
}
