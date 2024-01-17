package org.wordpress.android.modules

import com.gravatar.GravatarApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class GravatarModule {
    @Singleton
    @Provides
    fun provideGravatarApi(
    ): GravatarApi = GravatarApi()
}
