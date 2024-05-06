@file:OptIn(KspExperimental::class)


package org.wordpress.android.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import org.wordpress.android.annotation.Experiment
import org.wordpress.android.annotation.Feature
import org.wordpress.android.annotation.RemoteFieldDefaultGenerater

@OptIn(KspExperimental::class)
class RemoteConfigProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    /**
     * In the case of this processor, we only one need round. Generated files do not depend on each other
     * or any other processor.
     *
     * See: https://github.com/google/ksp/issues/797#issuecomment-1041127747
     * Also: https://github.com/google/ksp/blob/a0cd7774a7f65cec45a50ecc8960ef5e4d47fc21/examples/playground/test-processor/src/main/kotlin/TestProcessor.kt#L20
     */
    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }

        val remoteFeatures = resolver.getSymbolsWithAnnotation("org.wordpress.android.annotation.Feature")
            .toList()

        generateRemoteFeatureConfigDefaults(resolver, remoteFeatures)
        generateRemoteFieldsConfigDefaults(resolver)
        generateFeaturesInDevelopment(resolver)
        generateRemoteFeatureConfigCheck(remoteFeatures)

        invoked = true
        return emptyList()
    }

    private fun generateRemoteFeatureConfigDefaults(resolver: Resolver, remoteFeatures: List<KSAnnotated>) {
        val experiments = resolver.getSymbolsWithAnnotation("org.wordpress.android.annotation.Experiment")
            .toList()

        val defaults = (remoteFeatures + experiments)
            .map { element: KSAnnotated ->
                val featuresDefaults = element.getAnnotationsByType(Feature::class)
                    .toList().associate { annotation ->
                        annotation.remoteField to annotation.defaultValue.toString()
                    }
                val experimentsDefaults = element.getAnnotationsByType(Experiment::class).toList()
                    .toList().associate { annotation ->
                        annotation.remoteField to annotation.defaultVariant
                    }
                featuresDefaults + experimentsDefaults
            }.flatMap { it.toList() }
            .toMap()

        if (defaults.isNotEmpty()) {
            RemoteFeatureConfigDefaultsBuilder(defaults).getContent()
                .writeTo(
                    codeGenerator,
                    aggregating = true,
                    originatingKSFiles = remoteFeatures.map { it.containingFile!! }
                )
        }
    }

    private fun generateRemoteFieldsConfigDefaults(resolver: Resolver) {
        val remoteFields =
            resolver.getSymbolsWithAnnotation("org.wordpress.android.annotation.RemoteFieldDefaultGenerater")
                .toList()
        val remoteFieldDefaults = remoteFields
            .associate { element: KSAnnotated ->
                element.getAnnotationsByType(RemoteFieldDefaultGenerater::class)
                    .toList()
                    .first()
                    .let { annotation ->
                        annotation.remoteField to annotation.defaultValue
                    }
            }

        if(remoteFieldDefaults.isNotEmpty()) {
            RemoteFieldConfigDefaultsBuilder(remoteFieldDefaults).getContent()
                .writeTo(
                    codeGenerator,
                    aggregating = true,
                    originatingKSFiles = remoteFields.map { it.containingFile!! }
                )
        }
    }

    private fun generateFeaturesInDevelopment(resolver: Resolver) {
        val featuresInDevelopment =
            resolver.getSymbolsWithAnnotation("org.wordpress.android.annotation.FeatureInDevelopment")
                .filterIsInstance<KSClassDeclaration>()
                .toList()
        val featuresInDevelopmentDefaults = featuresInDevelopment
            .map { it.simpleName.asString() }

        if(featuresInDevelopmentDefaults.isNotEmpty()) {
            FeaturesInDevelopmentDefaultsBuilder(featuresInDevelopmentDefaults).getContent()
                .writeTo(
                    codeGenerator,
                    aggregating = true,
                    originatingKSFiles = featuresInDevelopment.map { it.containingFile!! }
                )
        }
    }

    private fun generateRemoteFeatureConfigCheck(remoteFeatures: List<KSAnnotated>) {
        if(remoteFeatures.isNotEmpty()) {
            RemoteFeatureConfigCheckBuilder(
                remoteFeatures.filterIsInstance<KSClassDeclaration>().map { it.asType(emptyList()).toTypeName() }
            ).getContent().writeTo(
                codeGenerator,
                aggregating = true,
                originatingKSFiles = remoteFeatures.map { it.containingFile!! }
            )
        }
    }
}
