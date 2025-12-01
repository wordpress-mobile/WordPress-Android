package org.wordpress.android.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import org.wordpress.android.fluxc.network.NetworkRequestsRetentionPeriod
import org.wordpress.android.fluxc.network.TrackNetworkRequestsInterceptor
import org.wordpress.android.fluxc.network.TrackNetworkRequestsPreference
import org.wordpress.android.ui.posts.editor.GutenbergKitNetworkLogger
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class TrackNetworkRequestsModule {
    @Singleton
    @Provides
    fun provideTrackNetworkRequestsPreference(appPrefsWrapper: AppPrefsWrapper): TrackNetworkRequestsPreference {
        return object : TrackNetworkRequestsPreference {
            override fun isEnabled(): Boolean = appPrefsWrapper.isTrackNetworkRequestsEnabled
            override fun getRetentionPeriod(): NetworkRequestsRetentionPeriod =
                NetworkRequestsRetentionPeriod.fromInt(appPrefsWrapper.trackNetworkRequestsRetentionPeriod)
        }
    }

    @Singleton
    @Provides
    fun provideTrackNetworkRequestsInterceptor(
        @ApplicationContext context: Context,
        preference: TrackNetworkRequestsPreference
    ): TrackNetworkRequestsInterceptor {
        return TrackNetworkRequestsInterceptor(context, preference)
    }

    @Provides
    @IntoSet
    @Named(OkHttpClientQualifiers.INTERCEPTORS)
    fun provideTrackNetworkRequestsInterceptorAsInterceptor(
        interceptor: TrackNetworkRequestsInterceptor
    ): Interceptor = interceptor

    @Singleton
    @Provides
    fun provideGutenbergKitNetworkLogger(
        interceptor: TrackNetworkRequestsInterceptor
    ): GutenbergKitNetworkLogger {
        return GutenbergKitNetworkLogger(interceptor)
    }
}
