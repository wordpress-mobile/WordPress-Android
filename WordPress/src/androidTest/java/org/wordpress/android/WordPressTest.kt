package org.wordpress.android

import org.wordpress.android.modules.AppComponent
import org.wordpress.android.modules.DaggerAppComponentTest

class WordPressTest : WordPress() {
    override fun initDaggerComponent() {
        mAppComponent = DaggerAppComponentTest.builder()
                .application(this)
                .build()
    }

    fun setAppComponent(appComponent: AppComponent) {
        mAppComponent = appComponent
    }
}
