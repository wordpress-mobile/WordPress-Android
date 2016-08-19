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
    private static final String WPCOMREST_ENDPOINT_FILE = "fluxc/src/main/tools/wp-com-endpoints.txt";
    private static final String XMLRPC_ENDPOINT_FILE = "fluxc/src/main/tools/xmlrpc-endpoints.txt";

    private Filer mFiler;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        mFiler = processingEnv.getFiler();

        try {
            generateWPCOMRESTEndpointFile();
            generateXMLRPCEndpointFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return true;
    }

    private void generateWPCOMRESTEndpointFile() throws IOException {
        File file = new File(WPCOMREST_ENDPOINT_FILE);
        EndpointNode rootNode = EndpointTreeGenerator.process(file);

        TypeSpec endpointClass = RESTPoet.generate(rootNode, "WPCOMREST", WPComEndpoint.class);
        writeEndpointClassToFile(endpointClass);
    }

    private void generateXMLRPCEndpointFile() throws IOException {
        File file = new File(XMLRPC_ENDPOINT_FILE);

        TypeSpec endpointClass = XMLRPCPoet.generate(file, "XMLRPC");
        writeEndpointClassToFile(endpointClass);
    }

    private void writeEndpointClassToFile(TypeSpec endpointClass) throws IOException {
        JavaFile javaFile = JavaFile.builder(AnnotationConfig.PACKAGE_ENDPOINTS, endpointClass)
                .indent("    ")
                .build();

        javaFile.writeTo(mFiler);
    }
}
