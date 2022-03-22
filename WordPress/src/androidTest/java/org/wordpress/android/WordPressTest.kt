package org.wordpress.android

import org.wordpress.android.modules.DaggerAppComponentTest

class WordPressTest : WordPress() {
    override fun initDaggerComponent() {
        appComponent = DaggerAppComponentTest.builder()
                .application(this)
                .build()
    }
}
