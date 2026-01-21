package org.wordpress.android.modules

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsDataSourceImpl

@InstallIn(SingletonComponent::class)
@Module
abstract class StatsModule {
    @Binds
    abstract fun bindStatsDataSource(impl: StatsDataSourceImpl): StatsDataSource
}
