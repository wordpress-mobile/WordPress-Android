package org.wordpress.android.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.junit.Test

class RemoteConfigProcessorTest {
    @Test
    fun `given a class with features annotation, when compiling, generate expected configuration check`() {
        // when
        val result = compile(listOf(featureA))

        // then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result.classLoader.loadClass("org.wordpress.android.util.config.RemoteFeatureConfigCheck"))
            .hasDeclaredMethods("checkRemoteFields")
    }

    @Test
    fun `given a class with remote field annotation, when compiling, generate expected config defaults class`() {
        // given
        val remoteFieldA = SourceFile.kotlin(
            "RemoteField.kt", """
        import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
        import org.wordpress.android.util.config.AppConfig

        @RemoteFieldDefaultGenerater(remoteField = "remoteField", defaultValue = "default")
        class RemoteFieldA
        """
        )

        // when
        val result = compile(listOf(remoteFieldA))

        // then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val remoteFieldConfigDefaultsClass =
            result.classLoader.loadClass("org.wordpress.android.util.config.RemoteFieldConfigDefaults")
        val remoteFieldConfigDefaultsObject = remoteFieldConfigDefaultsClass.kotlin.objectInstance

        assertThat(
            remoteFieldConfigDefaultsClass.getDeclaredField("remoteFieldConfigDefaults")
                .apply { isAccessible = true }
                .get(remoteFieldConfigDefaultsObject)
                .cast<Map<String, Any>>()
        ).containsEntry("remoteField", "default")
    }

    @Test
    fun `given class with feature and experiment annotation, when compiling, generate config defaults class`() {
        // given
        val experiment = SourceFile.kotlin(
            "Experiment.kt", """
        import org.wordpress.android.annotation.Experiment
        import org.wordpress.android.util.config.AppConfig

        @Experiment("experimentFeature", "defaultVariant")
        class Experiment
        """
        )

        // when
        val result = compile(listOf(featureA, experiment))

        // then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val remoteFieldConfigDefaultsClass =
            result.classLoader.loadClass("org.wordpress.android.util.config.RemoteFeatureConfigDefaults")
        val remoteFieldConfigDefaultsObject = remoteFieldConfigDefaultsClass.kotlin.objectInstance

        assertThat(
            remoteFieldConfigDefaultsClass.getDeclaredField("remoteFeatureConfigDefaults")
                .apply { isAccessible = true }
                .get(remoteFieldConfigDefaultsObject)
                .cast<Map<String, Any>>()
        ).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "experimentFeature" to "defaultVariant",
                "remoteField" to "false"
            )
        )
    }

    @Test
    fun `given class with feature in development annotation, when compiling, generate expected list of classes`() {
        // given
        val experiment = SourceFile.kotlin(
            "Experiment.kt", """
        import org.wordpress.android.annotation.FeatureInDevelopment
        import org.wordpress.android.util.config.AppConfig

        @FeatureInDevelopment
        class DevFeature
        """
        )

        // when
        val result = compile(listOf(experiment))

        // then

        val featuresInDevelopmentClass =
            result.classLoader.loadClass("org.wordpress.android.util.config.FeaturesInDevelopment")
        val featuresInDevelopmentObject = featuresInDevelopmentClass.kotlin.objectInstance
        assertThat(
            featuresInDevelopmentClass.getDeclaredField("featuresInDevelopment")
                .apply { isAccessible = true }
                .get(featuresInDevelopmentObject)
                .cast<List<String>>()
        ).containsOnly("DevFeature")
    }

    private fun compile(src: List<SourceFile>) = KotlinCompilation().apply {
        sources = src + fakeAppConfig
        symbolProcessorProviders = listOf(RemoteConfigProcessorProvider())
        kspWithCompilation = true
        inheritClassPath = true
        messageOutputStream = System.out
    }.compile()

    // Fake AppConfig is needed, as it's a class that is expected to be present in the classpath. Originally, this class
    // is placed in `WordPress` module.
    private val fakeAppConfig = SourceFile.kotlin(
        "AppConfig.kt", """
        package org.wordpress.android.util.config

        class AppConfig
    """
    )

    private val featureA = SourceFile.kotlin(
        "Feature.kt", """
        import org.wordpress.android.annotation.Feature
        import org.wordpress.android.util.config.AppConfig

        @Feature("remoteField", false)
        class FeatureA(appConfig: AppConfig, val remoteField: String ="foo")
        """
    )
}
