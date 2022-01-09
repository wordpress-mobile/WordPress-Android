package org.wordpress.android

import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

open class WordPressApplication : WordPress(), HasAndroidInjector {
    @Inject lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject lateinit var initializer: AppInitializer

    override fun onCreate() {
        super.onCreate()

        appInitializer.init()
    }

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    override fun getAppInitializer() = initializer
}
