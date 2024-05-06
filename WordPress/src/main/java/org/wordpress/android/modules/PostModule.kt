package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.ui.posts.IPostFreshnessChecker
import org.wordpress.android.ui.posts.PostFreshnessCheckerImpl
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class PostModule {
    @Singleton
    @Provides
    fun providePostFreshnessChecker(): IPostFreshnessChecker = PostFreshnessCheckerImpl()
}
