package org.wordpress.android.ui.stats.refresh.lists.sections.granular

import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase

interface GranularUseCaseFactory {
    fun build(granularity: StatsGranularity): BaseStatsUseCase<*, *>
}
