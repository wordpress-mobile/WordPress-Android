package org.wordpress.android.fluxc.module

import android.content.Context
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao
import org.wordpress.android.fluxc.persistence.JetpackCPConnectedSitesDao
import org.wordpress.android.fluxc.persistence.PlanOffersDao
import org.wordpress.android.fluxc.persistence.RemoteConfigDao
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase.Companion.buildDb
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeObjectivesDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingDao
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao
import org.wordpress.android.fluxc.persistence.comments.CommentsDao
import org.wordpress.android.fluxc.persistence.dashboard.CardsDao
import org.wordpress.android.fluxc.persistence.domains.DomainDao
import org.wordpress.android.fluxc.persistence.jetpacksocial.JetpackSocialDao
import javax.inject.Singleton

@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(context: Context): WPAndroidDatabase {
        return buildDb(context)
    }

    @Singleton
    @Provides
    fun provideBloggingRemindersDao(wpAndroidDatabase: WPAndroidDatabase): BloggingRemindersDao {
        return wpAndroidDatabase.bloggingRemindersDao()
    }

    @Singleton
    @Provides
    fun providePlanOffersDao(wpAndroidDatabase: WPAndroidDatabase): PlanOffersDao {
        return wpAndroidDatabase.planOffersDao()
    }

    @Singleton
    @Provides
    fun provideCommentsDao(wpAndroidDatabase: WPAndroidDatabase): CommentsDao {
        return wpAndroidDatabase.commentsDao()
    }

    @Singleton
    @Provides
    fun provideDashboardCardsDao(wpAndroidDatabase: WPAndroidDatabase): CardsDao {
        return wpAndroidDatabase.dashboardCardsDao()
    }

    @Singleton
    @Provides
    fun provideBloggingPromptsDao(wpAndroidDatabase: WPAndroidDatabase): BloggingPromptsDao {
        return wpAndroidDatabase.bloggingPromptsDao()
    }

    @Singleton
    @Provides
    fun provideFeatureFlagConfigDao(wpAndroidDatabase: WPAndroidDatabase): FeatureFlagConfigDao {
        return wpAndroidDatabase.featureFlagConfigDao()
    }

    @Singleton
    @Provides
    fun provideRemoteConfigDao(wpAndroidDatabase: WPAndroidDatabase): RemoteConfigDao {
        return wpAndroidDatabase.remoteConfigDao()
    }

    @Singleton
    @Provides
    fun provideDomainsDao(wpAndroidDatabase: WPAndroidDatabase): DomainDao {
        return wpAndroidDatabase.domainDao()
    }

    @Singleton
    @Provides
    fun provideJetpackConnectedSitesDao(
        wpAndroidDatabase: WPAndroidDatabase
    ): JetpackCPConnectedSitesDao {
        return wpAndroidDatabase.jetpackCPConnectedSitesDao()
    }

    @Singleton
    @Provides
    fun provideBlazeCampaignsDao(wpAndroidDatabase: WPAndroidDatabase): BlazeCampaignsDao {
        return wpAndroidDatabase.blazeCampaignsDao()
    }

    @Singleton
    @Provides
    fun provideBlazeTargetingDao(wpAndroidDatabase: WPAndroidDatabase): BlazeTargetingDao {
        return wpAndroidDatabase.blazeTargetingDao()
    }

    @Singleton
    @Provides
    fun provideBlazeObjectivesDao(wpAndroidDatabase: WPAndroidDatabase): BlazeObjectivesDao {
        return wpAndroidDatabase.blazeObjectivesDao()
    }

    @Singleton
    @Provides
    fun provideJetpackSocialDao(wpAndroidDatabase: WPAndroidDatabase): JetpackSocialDao {
        return wpAndroidDatabase.jetpackSocialDao()
    }
}
