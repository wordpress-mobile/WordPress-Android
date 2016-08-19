package org.wordpress.android.fluxc.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import org.wordpress.android.fluxc.annotations.Endpoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

import javax.lang.model.element.Modifier;

public class XMLRPCPoet {
    public static TypeSpec generate(File endpointFile, String fileName) throws IOException {
        TypeSpec.Builder XMLRPCBuilder = TypeSpec.enumBuilder(fileName)
                .addModifiers(Modifier.PUBLIC)
                .addField(String.class, "mEndpoint", Modifier.PRIVATE, Modifier.FINAL);

        MethodSpec XMLRPConstructor = MethodSpec.constructorBuilder()
                .addParameter(String.class, "endpoint")
                .addStatement("mEndpoint = endpoint")
                .build();

        XMLRPCBuilder.addMethod(XMLRPConstructor);

        MethodSpec XMLRPCToString = MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $L", "mEndpoint")
                .addAnnotation(Override.class)
                .build();

        XMLRPCBuilder.addMethod(XMLRPCToString);

        BufferedReader in = new BufferedReader(new FileReader(endpointFile));

        String fullEndpoint;
        while ((fullEndpoint = in.readLine()) != null) {
            if (fullEndpoint.length() == 0) {
                continue;
            }
            String endpointName = fullEndpoint.split("\\.")[1];
            String enumName = endpointName.replaceAll("([a-z])([A-Z]+)", "$1_$2").toUpperCase(Locale.US);

            XMLRPCBuilder.addEnumConstant(enumName, TypeSpec.anonymousClassBuilder("$S", fullEndpoint)
                    .addAnnotation(AnnotationSpec.builder(Endpoint.class)
                            .addMember("value", "$S", fullEndpoint)
                            .build())
                    .build());
        }

        in.close();

        return XMLRPCBuilder.build();
    }
}
