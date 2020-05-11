package org.wordpress.android.processor

import org.wordpress.android.annotation.Experiment
import com.google.auto.service.AutoService
import org.wordpress.android.annotation.RemoteConfig
import org.wordpress.android.processor.ExperimentProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class) // For registering the service
@SupportedSourceVersion(SourceVersion.RELEASE_8) // to support Java 8
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ExperimentProcessor : AbstractProcessor() {
    override fun process(p0: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment?): Boolean {
        println("Vojta: Starting processing")
        printToFile("annotatedElements: ${roundEnvironment?.toString()}")
        roundEnvironment?.getElementsAnnotatedWith(Experiment::class.java)?.let { annotatedElements ->
            annotatedElements.map { element ->
                val annotation = element.getAnnotation(Experiment::class.java)
                println("Vojta: Annotations found")
                annotation.remoteField to annotation.defaultVariant
            }
                    .toMap()
                    .apply {
                        println("Vojta: Generating files")
                        generateRemoteConfigDefaults(this)
                    }
        }
        return true
    }

    private fun printToFile(text: String) {
        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        val file = File(kaptKotlinGeneratedDir, "prdel.kt")
        file.writeText(text)
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Experiment::class.java.name, RemoteConfig::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    private fun generateRemoteConfigDefaults(remoteConfigDefaults: Map<String, String>){
        val fileName = "prdel"
        val fileContent = RemoteConfigDefaultsBuilder(remoteConfigDefaults).getContent()

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        val file = File(kaptKotlinGeneratedDir, "$fileName.xml")

        file.writeText(fileContent)
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}
