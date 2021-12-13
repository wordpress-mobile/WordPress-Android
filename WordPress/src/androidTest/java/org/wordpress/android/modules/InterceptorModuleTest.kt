package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import org.wordpress.android.support.MockingInterceptor
import javax.inject.Named

@TestInstallIn(components = [SingletonComponent::class], replaces = [InterceptorModule::class])
@Module
class InterceptorModuleTest {
    @Provides @IntoSet @Named("interceptors")
    fun provideMockingInterceptor(): Interceptor = MockingInterceptor()
}
