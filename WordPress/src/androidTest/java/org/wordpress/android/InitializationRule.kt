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
        fun appInitializer(): AppInitializer
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                val instrumentation = InstrumentationRegistry.getInstrumentation()

                val application = instrumentation.targetContext.applicationContext as Application
                val appInitializer = EntryPoints.get(
                        application,
                        AppInitializerEntryPoint::class.java
                ).appInitializer()
                instrumentation.runOnMainSync { appInitializer.init() }

                base?.evaluate()
            }
        }
    }
}
