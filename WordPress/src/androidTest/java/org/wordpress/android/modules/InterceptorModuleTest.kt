package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import org.wordpress.android.support.MockingInterceptor
import javax.inject.Named

@InstallIn(SingletonComponent::class)
@Module
class InterceptorModuleTest {
    @Provides @IntoSet @Named("interceptors")
    fun provideMockingInterceptor(): Interceptor = MockingInterceptor()
}
