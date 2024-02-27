package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.ui.posts.IPostFreshnessChecker
import org.wordpress.android.ui.posts.PostFreshnessCheckerImpl

@InstallIn(SingletonComponent::class)
@Module
class PostModule {
    @Provides
    fun providePostFreshnessChecker(): IPostFreshnessChecker = PostFreshnessCheckerImpl()
}
