package org.wordpress.android

class WordPressTest : WordPress() {
    override fun initDaggerComponent() {
        mAppComponent = DaggerAppComponentTest.builder()
                .application(this)
                .build()
    }
}
