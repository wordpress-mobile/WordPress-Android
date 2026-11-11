package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.datasets.SiteSettingsProvider
import org.wordpress.android.datasets.SiteSettingsProviderImpl
import org.wordpress.android.ui.posts.EditorServiceProvider
import org.wordpress.android.ui.posts.EditorServiceProviderImpl
import org.wordpress.android.ui.posts.IPostFreshnessChecker
import org.wordpress.android.ui.posts.PostFreshnessCheckerImpl
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class PostModule {
    @Singleton
    @Provides
    fun providePostFreshnessChecker(): IPostFreshnessChecker =
        PostFreshnessCheckerImpl()

    @Singleton
    @Provides
    fun provideSiteSettingsProvider(
        impl: SiteSettingsProviderImpl
    ): SiteSettingsProvider = impl

    @Singleton
    @Provides
    fun provideEditorServiceProvider(
        impl: EditorServiceProviderImpl
    ): EditorServiceProvider = impl
}
