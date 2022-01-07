package org.wordpress.android

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class InitializationRule : TestRule {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppInitializerEntryPoint {
        fun initializer(): AppInitializer
    }

    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                if (!isInitialized) {
                    instrumentation.runOnMainSync {
                        val application = instrumentation.targetContext.applicationContext as Application
                        val appInitializer = EntryPoints.get(
                                application,
                                AppInitializerEntryPoint::class.java
                        ).initializer()

                        appInitializer.init()
                    }
                    isInitialized = true
                }
                base?.evaluate()
            }
        }
    }

    companion object {
        // InitializationRule must be initialized only once. Otherwise some static functions in AppInitializer throws
        // exception, if they are initialized more than once. (e.g. WebView.setDataDirectorySuffix(),
        // EventBusBuilder.installDefaultEventBus)
        private var isInitialized = false
    }
}
