package org.wordpress.android.modules

import com.automattic.android.tracks.crashlogging.CrashLoggingOkHttpInterceptorProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import org.wordpress.android.fluxc.network.FluxCRequestFormatter
import javax.inject.Named

@InstallIn(SingletonComponent::class)
@Module
class HttpBreadcrumbModule {
    @Provides
    @IntoSet
    @Named(OkHttpClientQualifiers.NETWORK_INTERCEPTORS)
    fun provideCrashLoggingHttpInterceptor(): Interceptor {
        val requestFormatter = FluxCRequestFormatter()
        return CrashLoggingOkHttpInterceptorProvider.createInstance(requestFormatter)
    }
}
