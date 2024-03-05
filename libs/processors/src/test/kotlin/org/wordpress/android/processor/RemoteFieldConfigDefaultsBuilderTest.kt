package org.wordpress.android.processor

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteFieldConfigDefaultsBuilderTest {
    @Test
    fun `given a list of remote fields, when building the object, then generate list of remote fields`() {
        // given
        val keyA = "keyA"
        val valueA = "valueA"
        val keyB = "keyB"
        val valueB = "valueB"

        // when
        val sut = RemoteFieldConfigDefaultsBuilder(mapOf(keyA to valueA, keyB to valueB))

        // then
        assertEquals(
            """
            // Automatically generated file. DO NOT MODIFY
            package org.wordpress.android.util.config

            import kotlin.Any
            import kotlin.String
            import kotlin.collections.Map

            public object RemoteFieldConfigDefaults {
                public val remoteFieldConfigDefaults: Map<String, Any> = mapOf(
                        "$keyA" to "$valueA",
                        "$keyB" to "$valueB"
                        )
            }

        """.trimIndent(), sut.getContent().toString()
        )
    }

    @Test
    fun `given an empty list of remote fields, when building the object, then generate empty list of remote fields`() {
        // given
        val remoteFields = emptyMap<String, String>()

        // when
        val sut = RemoteFieldConfigDefaultsBuilder(remoteFields)

        // then
        assertEquals(
            """
                // Automatically generated file. DO NOT MODIFY
                package org.wordpress.android.util.config

                import kotlin.Any
                import kotlin.String
                import kotlin.collections.Map

                public object RemoteFieldConfigDefaults {
                    public val remoteFieldConfigDefaults: Map<String, Any> = mapOf(
                            )
                }

            """.trimIndent(), sut.getContent().toString()
        )
    }
}
