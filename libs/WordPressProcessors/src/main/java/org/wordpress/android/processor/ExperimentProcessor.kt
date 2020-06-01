package org.wordpress.android.processor

import com.google.auto.service.AutoService
import org.wordpress.android.annotation.Experiment
import org.wordpress.android.processor.ExperimentProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import java.io.File
import java.util.Locale
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
@SupportedAnnotationTypes("org.wordpress.android.annotation.Experiment")
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ExperimentProcessor : AbstractProcessor() {
    override fun process(p0: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment?): Boolean {
        roundEnvironment?.getElementsAnnotatedWith(Experiment::class.java)?.let { annotatedElements ->
            if (annotatedElements.size > 0) {
                annotatedElements.map { element ->
                    val annotation = element.getAnnotation(Experiment::class.java)
                    annotation.remoteField to annotation.defaultVariant
                }
                        .toMap()
                        .apply {
                            generateRemoteConfigDefaults(this)
                        }
            }
        }
        return true
    }

    private fun generateRemoteConfigDefaults(remoteConfigDefaults: Map<String, String>) {
        val fileContent = RemoteConfigDefaultsBuilder(remoteConfigDefaults).getContent()

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        var dir = File(kaptKotlinGeneratedDir, "../../../res/resValues/")
        dir.mkdirs()
        var test = ""
        kaptKotlinGeneratedDir?.split("/")?.last()?.let { version: String ->
            val name = version.replace(Regex("([A-Z][a-z]+)"), " $1")
                    .replace(Regex("([A-Z][A-Z]+)"), " $1")
                    .replace(Regex("([^A-Za-z ]+)"), " $1")
                    .trim().toLowerCase(Locale.getDefault())
            test = name
            for (dirName in name.split(" ")) {
                dir = File(dir, dirName)
                dir.mkdirs()
            }
        }
        dir = File(dir, "xml")
        dir.mkdirs()


        val file = File(dir, "remote_config_defaults.xml")
        file.writeText(fileContent)
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}
