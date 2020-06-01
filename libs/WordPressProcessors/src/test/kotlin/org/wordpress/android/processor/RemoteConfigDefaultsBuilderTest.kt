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

        assertThat(fileContent).isEqualTo(
                """
            <?xml version="1.0" encoding="utf-8"?>
            <defaultsMap>
            <!-- Automatically generated file. DO NOT MODIFY -->
            <entry>
                <key>$keyA</key>
                <value>$valueA</value>
            </entry>
            <entry>
                <key>$keyB</key>
                <value>$valueB</value>
            </entry>
            </defaultsMap>
            
        """.trimIndent()
        )
    }
}
