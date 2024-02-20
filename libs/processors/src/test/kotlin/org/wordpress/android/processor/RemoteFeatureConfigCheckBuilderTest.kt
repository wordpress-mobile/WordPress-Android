package org.wordpress.android.processor

import com.squareup.kotlinpoet.ClassName
import org.assertj.core.api.Assertions
import org.junit.Test

class RemoteFeatureConfigCheckBuilderTest {

    @Test
    fun `given feature classes, when building config check, then generate the correct checks`() {
        // given
        val classA = "customClassA"
        val classB = "customClassB"
        val features = listOf(
            ClassName("org.wordpress", listOf(classA)),
            ClassName("org.wordpress", listOf(classB))
        )

        // when
        val sut = RemoteFeatureConfigCheckBuilder(features)

        // then
        Assertions.assertThat(sut.getContent().toString()).isEqualTo(
            """
            // Automatically generated file. DO NOT MODIFY
            package org.wordpress.android.util.config

            import org.wordpress.$classA
            import org.wordpress.$classB

            class RemoteFeatureConfigCheck(
                val appConfig: AppConfig
            ) {
                val $classA: $classA = $classA(appConfig)

                val $classB: $classB = $classB(appConfig)

                fun checkRemoteFields() {
                    if ($classA.remoteField == null) {
                        throw IllegalArgumentException(""${'"'}org.wordpress.$classA needs to define
                            remoteField""${'"'})
                    }
                    if ($classB.remoteField == null) {
                        throw IllegalArgumentException(""${'"'}org.wordpress.$classB needs to define
                            remoteField""${'"'})
                    }
                }
            }

        """.trimIndent()
        )
    }
}
