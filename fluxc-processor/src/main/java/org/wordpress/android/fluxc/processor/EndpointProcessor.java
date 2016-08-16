package org.wordpress.android.fluxc.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import org.wordpress.android.fluxc.annotations.AnnotationConfig;
import org.wordpress.android.fluxc.annotations.endpoint.EndpointNode;
import org.wordpress.android.fluxc.annotations.endpoint.EndpointTreeGenerator;
import org.wordpress.android.fluxc.annotations.endpoint.WPComEndpoint;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

@SuppressWarnings("unused")
@AutoService(Processor.class)
public class EndpointProcessor extends AbstractProcessor {
    @Override
    public void init(ProcessingEnvironment processingEnv) {
        try {
            File file = new File("fluxc/src/main/tools/wp-com-endpoints.txt");
            EndpointNode rootNode = EndpointTreeGenerator.process(file);
            Filer filer = processingEnv.getFiler();

            TypeSpec apiClass = RESTPoet.generate(rootNode, "WPCOMREST", WPComEndpoint.class);

            JavaFile javaFile = JavaFile.builder(AnnotationConfig.PACKAGE + ".endpoint", apiClass)
                    .indent("    ")
                    .build();

            javaFile.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return true;
    }
}
