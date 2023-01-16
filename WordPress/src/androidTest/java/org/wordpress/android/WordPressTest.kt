package org.wordpress.android

import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.CustomTestApplication
import dagger.hilt.components.SingletonComponent

@CustomTestApplication(BaseWordPressTest::class)
interface WordPressTest

open class BaseWordPressTest : WordPress(), HasAndroidInjector {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AndroidInjectorEntryPoint {
        fun injector(): DispatchingAndroidInjector<Any>
    }

    lateinit var initializer: AppInitializer

    override fun androidInjector(): AndroidInjector<Any> = EntryPoints.get(
        applicationContext,
        AndroidInjectorEntryPoint::class.java
    ).injector()

    override fun initializer() = initializer
}
