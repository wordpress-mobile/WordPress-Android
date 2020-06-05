package org.wordpress.android.processor

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class RemoteConfigDefaultsBuilderTest {
    @Test
    fun `build a remote config file content`() {
        val keyA = "keyA"
        val valueA = "valueA"
        val keyB = "keyB"
        val valueB = "valueB"

        val remoteConfigDefaultsBuilder = RemoteConfigDefaultsBuilder(mapOf(keyA to valueA, keyB to valueB))

        val fileContent = remoteConfigDefaultsBuilder.getContent()

        assertThat(fileContent.toString()).isEqualTo(
                """
            // Automatically generated file. DO NOT MODIFY
            package org.wordpress.android.util.config
            
            import kotlin.Any
            import kotlin.String
            import kotlin.collections.Map
            
            object RemoteConfigDefaults {
              val remoteConfigDefaults: Map<String, Any> = mapOf(
                  "$keyA" to "$valueA",
                  "$keyB" to "$valueB"
                  )
            }
            
        """.trimIndent()
        )
    }
}
