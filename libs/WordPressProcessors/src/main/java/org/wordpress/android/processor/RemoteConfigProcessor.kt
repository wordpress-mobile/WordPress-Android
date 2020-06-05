package org.wordpress.android.processor

import com.google.auto.service.AutoService
import org.wordpress.android.annotation.Experiment
import org.wordpress.android.annotation.Feature
import java.io.File
import java.lang.Exception
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind

@AutoService(Processor::class) // For registering the service
@SupportedSourceVersion(SourceVersion.RELEASE_8) // to support Java 8
@SupportedAnnotationTypes(
        "org.wordpress.android.annotation.Experiment",
        "org.wordpress.android.annotation.Feature"
)
class RemoteConfigProcessor : AbstractProcessor() {
    override fun process(p0: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment?): Boolean {
        val experiments = roundEnvironment?.getElementsAnnotatedWith(Experiment::class.java)?.map { element ->
            val annotation = element.getAnnotation(Experiment::class.java)
            annotation.remoteField to annotation.defaultVariant
        } ?: listOf()
        val features = roundEnvironment?.getElementsAnnotatedWith(Feature::class.java)?.map { element ->
            val annotation = element.getAnnotation(Feature::class.java)
            annotation.remoteField to annotation.defaultValue.toString()
        } ?: listOf()
        return if (experiments.isNotEmpty() || features.isNotEmpty()) {
            generateRemoteConfigDefaults((experiments + features).toMap())
            true
        } else {
            false
        }
    }

    private fun generateRemoteConfigDefaults(
        remoteConfigDefaults: Map<String, String>
    ) {
        try {
            val fileContent = RemoteConfigDefaultsBuilder(remoteConfigDefaults).getContent()

            val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            fileContent.writeTo(File(kaptKotlinGeneratedDir))
        } catch (e: Exception) {
            processingEnv.messager.printMessage(Kind.ERROR, "Failed to generate remote_config_defaults")
        }
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}
