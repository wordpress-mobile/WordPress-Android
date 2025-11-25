package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import org.wordpress.android.fluxc.network.TrackNetworkRequestsInterceptor
import org.wordpress.android.fluxc.network.TrackNetworkRequestsPreference
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
        }
    }

    @Singleton
    @Provides
    @IntoSet
    @Named("interceptors")
    fun provideTrackNetworkRequestsInterceptor(preference: TrackNetworkRequestsPreference): Interceptor {
        return TrackNetworkRequestsInterceptor(preference)
    }
}
