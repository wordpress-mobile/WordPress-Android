package org.wordpress.android.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import java.util.Locale

class RemoteConfigCheckBuilder(private val remoteFeatures: List<TypeName>) {
    @ExperimentalStdlibApi
    fun getContent(): FileSpec {
        val remoteFeaturesWithNames = remoteFeatures.map {
            it.toString()
                    .substringAfterLast(".")
                    .decapitalize(Locale.ROOT) to it
        }
        val propertySpecs = remoteFeaturesWithNames.map { remoteFeature ->
            val constructor = remoteFeature.second.toString().substringAfterLast(".")
            PropertySpec.builder(remoteFeature.first, remoteFeature.second)
                    .initializer(CodeBlock.of("$constructor(appConfig)".trimIndent()))
                    .build()
        }
        val remoteConfigDefaults = TypeSpec.classBuilder("RemoteConfigCheck")
                .addProperties(propertySpecs)
                .primaryConstructor(
                        listOf(
                                PropertySpec.builder(
                                        "appConfig",
                                        ClassName.bestGuess("org.wordpress.android.util.config.AppConfig")
                                ).build()
                        )
                )
                .addFunction(
                        FunSpec.builder("checkRemoteFields")
                                .addCode(buildCheckFunction(remoteFeaturesWithNames))
                                .build()
                )
                .build()
        return FileSpec.builder("org.wordpress.android.util.config", "RemoteConfigCheck")
                .addType(remoteConfigDefaults)
                .addComment("Automatically generated file. DO NOT MODIFY")
                .indent("    ")
                .build()
    }

    private fun buildCheckFunction(remoteFeatures: List<Pair<String, TypeName>>): CodeBlock {
        val stringBuilder = StringBuilder()
        remoteFeatures.forEach { feature ->
            stringBuilder.appendln("if (${feature.first}.remoteField == null) {")
            val error = "    throw IllegalArgumentException(\"\"\"${feature.second} needs to define remoteField\"\"\")"
            stringBuilder.appendln(error)
            stringBuilder.appendln("}")
        }
        return CodeBlock.of(stringBuilder.toString().trimIndent())
    }

    private fun TypeSpec.Builder.primaryConstructor(properties: List<PropertySpec>): TypeSpec.Builder {
        val propertySpecs = properties.map { p -> p.toBuilder().initializer(p.name).build() }
        val parameters = propertySpecs.map { ParameterSpec.builder(it.name, it.type).build() }
        val constructor = FunSpec.constructorBuilder()
                .addParameters(parameters)
                .build()

        return this
                .primaryConstructor(constructor)
                .addProperties(propertySpecs)
    }
}
