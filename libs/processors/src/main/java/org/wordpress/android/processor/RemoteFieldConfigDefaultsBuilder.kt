package org.wordpress.android.processor

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

private const val FILE_NAME = "RemoteFieldConfigDefaults"
private const val VARIABLE_NAME = "remoteFieldConfigDefaults"

class RemoteFieldConfigDefaultsBuilder(private val defaults: Map<String, String>) {
    fun getContent(): FileSpec {
        val map = Map::class.asClassName()
            .parameterizedBy(String::class.asClassName(), Any::class.asClassName())
        val stringBuilder = StringBuilder()
        defaults.keys.forEachIndexed { index, key ->
            stringBuilder.append("\n")
            stringBuilder.append("\"$key\" to \"${defaults[key]}\"")
            if (index < defaults.keys.size - 1) {
                stringBuilder.append(",")
            }
        }
        stringBuilder.append("\n")
        val remoteConfigDefaults = TypeSpec.objectBuilder("RemoteFieldConfigDefaults")
            .addProperty(
                PropertySpec.builder(VARIABLE_NAME, map)
                    .initializer("mapOf($stringBuilder)")
                    .build()
            )
            .build()
        return FileSpec.builder("org.wordpress.android.util.config", FILE_NAME)
            .addType(remoteConfigDefaults)
            .addComment("Automatically generated file. DO NOT MODIFY")
            .indent("    ")
            .build()
    }
}
