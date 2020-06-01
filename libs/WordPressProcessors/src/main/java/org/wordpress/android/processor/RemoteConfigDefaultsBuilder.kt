package org.wordpress.android.processor

class RemoteConfigDefaultsBuilder(private val defaults: Map<String, String>) {
    fun getContent(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        stringBuilder.append("<defaultsMap>\n")
        stringBuilder.append("<!-- Automatically generated file. DO NOT MODIFY -->\n")
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
        stringBuilder.append("</defaultsMap>\n")
        return stringBuilder.toString()
    }
}
