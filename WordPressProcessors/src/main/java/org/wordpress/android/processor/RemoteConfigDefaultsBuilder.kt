package org.wordpress.android.processor

class RemoteConfigDefaultsBuilder(private val defaults: Map<String, String>) {
    fun getContent(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        stringBuilder.append("<defaultsMap>")
        defaults.forEach {
            stringBuilder.append(
                    """
                <entry>
                    <key>${it.key}</key>
                    <value>${it.value}</value>
                    </entry>
                    """.trimIndent()
            )
        }
        stringBuilder.append("</defaultsMap>")
        return stringBuilder.toString()

    }
}
