package org.wordpress.android

import android.app.Application
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

open class BaseWordPressTest : Application(), HasAndroidInjector {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AndroidInjectorEntryPoint {
        fun injector(): DispatchingAndroidInjector<Any>
    }

    private lateinit var injector: AndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> {
        if (!this::injector.isInitialized) {
            injector = EntryPoints.get(
                    applicationContext,
                    AndroidInjectorEntryPoint::class.java
            ).injector()
        }
        return injector
    }
}
