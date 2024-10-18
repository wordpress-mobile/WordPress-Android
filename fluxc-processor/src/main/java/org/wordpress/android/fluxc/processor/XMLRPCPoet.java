package org.wordpress.android.fluxc.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import org.wordpress.android.fluxc.annotations.Endpoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.lang.model.element.Modifier;

public class XMLRPCPoet {
    public static TypeSpec generate(InputStream inputStream, String fileName, Map<String, List<String>> aliases)
            throws IOException {
        TypeSpec.Builder xmlrpcBuilder = TypeSpec.enumBuilder(fileName)
                .addModifiers(Modifier.PUBLIC)
                .addField(String.class, "mEndpoint", Modifier.PRIVATE, Modifier.FINAL);

        MethodSpec xmlrpcConstructor = MethodSpec.constructorBuilder()
                .addParameter(String.class, "endpoint")
                .addStatement("mEndpoint = endpoint")
                .build();

        xmlrpcBuilder.addMethod(xmlrpcConstructor);

        MethodSpec xmlrpcToString = MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $L", "mEndpoint")
                .addAnnotation(Override.class)
                .build();

        xmlrpcBuilder.addMethod(xmlrpcToString);

        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String fullEndpoint;
        while ((fullEndpoint = in.readLine()) != null) {
            if (fullEndpoint.length() == 0) {
                continue;
            }
            String endpointName = fullEndpoint.split("\\.")[1];
            String enumName = endpointName.replaceAll("([a-z])([A-Z]+)", "$1_$2").toUpperCase(Locale.US);

            xmlrpcBuilder.addEnumConstant(enumName, TypeSpec.anonymousClassBuilder("$S", fullEndpoint)
                    .addAnnotation(AnnotationSpec.builder(Endpoint.class)
                            .addMember("value", "$S", fullEndpoint)
                            .build())
                    .build());
        }

        // Add endpoint aliases
        for (String endpoint : aliases.keySet()) {
            for (String alias : aliases.get(endpoint)) {
                xmlrpcBuilder.addEnumConstant(alias, TypeSpec.anonymousClassBuilder("$S", endpoint)
                        .addAnnotation(AnnotationSpec.builder(Endpoint.class)
                                .addMember("value", "$S", endpoint)
                                .build())
                        .build());
            }
        }

        in.close();

        return xmlrpcBuilder.build();
    }
}
