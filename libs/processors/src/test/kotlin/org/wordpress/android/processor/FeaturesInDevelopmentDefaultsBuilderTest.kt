package org.wordpress.android.processor

import org.assertj.core.api.Assertions
import org.junit.Test

class FeaturesInDevelopmentDefaultsBuilderTest {
    @Test
    fun `given a list of features in development, when building the object, then generate the correct list`() {
        // given
        val featureA = "valueA"
        val featureB = "valueB"
        val features = listOf(featureA, featureB)

        // when
        val sut = FeaturesInDevelopmentDefaultsBuilder(features)

        // then
        Assertions.assertThat(sut.getContent().toString()).isEqualTo(
            """
            // Automatically generated file. DO NOT MODIFY
            package org.wordpress.android.util.config

            import kotlin.String
            import kotlin.collections.List

            public object FeaturesInDevelopment {
                public val featuresInDevelopment: List<String> = listOf(
                        "$featureA",
                        "$featureB"
                        )
            }

        """.trimIndent()
        )
    }

    @Test
    fun `given an empty list of features in development, when building the object, then generate empty list`() {
        // given
        val features = emptyList<String>()

        // when
        val sut = FeaturesInDevelopmentDefaultsBuilder(features)

        // then
        Assertions.assertThat(sut.getContent().toString()).isEqualTo(
            """
                // Automatically generated file. DO NOT MODIFY
                package org.wordpress.android.util.config

                import kotlin.String
                import kotlin.collections.List

                public object FeaturesInDevelopment {
                    public val featuresInDevelopment: List<String> = listOf(
                            )
                }

            """.trimIndent()
        )
    }
}
