package org.wordpress.android.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RemoteConfigProcessorTest {

    @Test
    fun `given a class with features annotation, when compiling, generate expected configuration check`() {
        // given
        val featureA = SourceFile.kotlin(
            "Feature.kt", """
        import org.wordpress.android.annotation.Feature
        import org.wordpress.android.util.config.AppConfig

        @Feature("remoteField", false)
        class A(appConfig: AppConfig, val remoteField: String ="foo")
        """
        )

        // when
        val result = compile(listOf(featureA))

        // then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result.classLoader.loadClass("org.wordpress.android.util.config.RemoteFeatureConfigCheck"))
            .hasDeclaredMethods("checkRemoteFields")
    }

    private fun compile(src: List<SourceFile>) = KotlinCompilation().apply {
        sources = src + fakeAppConfig
        annotationProcessors = listOf(RemoteConfigProcessor())
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

}
