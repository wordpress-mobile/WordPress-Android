package org.wordpress.android.fluxc.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.wordpress.android.fluxc.annotations.Endpoint;
import org.wordpress.android.fluxc.annotations.endpoint.EndpointNode;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.lang.model.element.Modifier;

public class RESTPoet {
    // TODO: Need to support "/sites/$site/posts/slug:$post_slug"-type endpoints
    private static final String[] JAVA_KEYWORDS = { "new", "abstract", "assert", "boolean",
            "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "extends", "false",
            "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native",
            "null", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true",
            "try", "void", "volatile", "while" };

    private static TypeName sBaseEndpointClass;

    public static void generate(EndpointNode rootNode, String fileName, String packageName, Class baseEndpointClass) {
        sBaseEndpointClass = ClassName.get(baseEndpointClass);

        TypeSpec.Builder WPCOMRESTBuilder = TypeSpec.classBuilder(fileName)
                .addModifiers(Modifier.PUBLIC);

        for (EndpointNode endpoint : rootNode.getChildren()) {
            addEndpointToBuilder(endpoint, WPCOMRESTBuilder);
        }

        JavaFile javaFile = JavaFile.builder(packageName, WPCOMRESTBuilder.build())
                .indent("    ")
                .build();

        try {
            File file = new File("fluxc/src/main/java/");
            javaFile.writeTo(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addEndpointToBuilder(EndpointNode endpointNode, TypeSpec.Builder classBuilder) {
        if (endpointNode.getLocalEndpoint().contains("$")) {
            processVariableEndpointNode(endpointNode, classBuilder);
        } else {
            processStaticEndpointNode(endpointNode, classBuilder);
        }
    }

    private static void processStaticEndpointNode(EndpointNode endpointNode, TypeSpec.Builder classBuilder) {
        String endpointName = endpointNode.getCleanEndpointName();
        String javaSafeEndpointName = underscoreIfJavaKeyword(endpointName);

        if (!endpointNode.hasChildren()) {
            // Build annotated accessor field for the static endpoint
            FieldSpec.Builder endpointFieldBuilder = FieldSpec.builder(sBaseEndpointClass, javaSafeEndpointName)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(Endpoint.class)
                            .addMember("value", "$S", endpointNode.getFullEndpoint())
                            .build());

            if (endpointNode.getParent().isRoot()) {
                endpointFieldBuilder.addModifiers(Modifier.STATIC)
                        .initializer("new $T($S)", sBaseEndpointClass, endpointNode.getLocalEndpoint());
            } else {
                endpointFieldBuilder
                        .initializer("new $T(getEndpoint() + $S)", sBaseEndpointClass, endpointNode.getLocalEndpoint());
            }

            classBuilder.addField(endpointFieldBuilder.build());
        } else {
            // Build constant String defining the local endpoint for this class
            FieldSpec endpointField = FieldSpec.builder(String.class, endpointName.toUpperCase(Locale.US) + "_ENDPOINT")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", endpointNode.getLocalEndpoint())
                    .build();

            MethodSpec endpointConstructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(String.class, "previousEndpoint")
                    .addStatement("super($L + $L)", "previousEndpoint", endpointField.name)
                    .build();

            TypeSpec.Builder endpointClassBuilder = TypeSpec.classBuilder(capitalize(endpointName) + "Endpoint")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .superclass(sBaseEndpointClass)
                    .addField(endpointField)
                    .addMethod(endpointConstructor);

            TypeName endpointClassName = ClassName.get("", capitalize(endpointName) + "Endpoint");

            // Build annotated accessor field for the static endpoint
            FieldSpec.Builder endpointFieldBuilder = FieldSpec.builder(endpointClassName, javaSafeEndpointName)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(Endpoint.class)
                            .addMember("value", "$S", endpointNode.getFullEndpoint())
                            .build());

            if (endpointNode.getParent().isRoot()) {
                endpointFieldBuilder.addModifiers(Modifier.STATIC)
                        .initializer("new $T($S)", endpointClassName, "/");
            } else {
                endpointFieldBuilder
                        .initializer("new $T(getEndpoint())", endpointClassName);
            }

            for (EndpointNode childEndpoint : endpointNode.getChildren()) {
                addEndpointToBuilder(childEndpoint, endpointClassBuilder);
            }

            classBuilder.addField(endpointFieldBuilder.build())
                    .addType(endpointClassBuilder.build());
        }
    }

    private static void processVariableEndpointNode(EndpointNode endpointNode, TypeSpec.Builder classBuilder) {
        String endpointName = endpointNode.getCleanEndpointName();

        if (!endpointNode.hasChildren()) {
            // Build annotated accessor method for variable endpoint and add it to the class
            classBuilder.addMethod(generateEndpointMethodForClass(endpointNode, sBaseEndpointClass));
        } else {
            MethodSpec endpointConstructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(String.class, "previousEndpoint")
                    .addParameter(long.class, endpointName + "Id")
                    .addStatement("super($L, $L)", "previousEndpoint", endpointName + "Id")
                    .build();

            TypeSpec.Builder endpointClassBuilder = TypeSpec.classBuilder(capitalize(endpointName) + "Endpoint")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .superclass(sBaseEndpointClass)
                    .addMethod(endpointConstructor);

            TypeName endpointClassName = ClassName.get("", capitalize(endpointName) + "Endpoint");

            // Build annotated accessor method for variable endpoint
            MethodSpec endpointMethod = generateEndpointMethodForClass(endpointNode, endpointClassName);

            for (EndpointNode childEndpoint : endpointNode.getChildren()) {
                addEndpointToBuilder(childEndpoint, endpointClassBuilder);
            }

            classBuilder.addMethod(endpointMethod)
                    .addType(endpointClassBuilder.build());
        }
    }

    private static MethodSpec generateEndpointMethodForClass(EndpointNode endpointNode, TypeName endpointClassName) {
        String endpointName = endpointNode.getCleanEndpointName();

        MethodSpec.Builder endpointMethodBuilder = MethodSpec.methodBuilder(endpointName)
                .addModifiers(Modifier.PUBLIC)
                .returns(endpointClassName)
                .addParameter(long.class, endpointName + "Id")
                .addAnnotation(AnnotationSpec.builder(Endpoint.class)
                        .addMember("value", "$S", endpointNode.getFullEndpoint())
                        .build());

        if (endpointNode.getParent().isRoot()) {
            endpointMethodBuilder.addModifiers(Modifier.STATIC)
                    .addStatement("return new $T($S, $L)", endpointClassName, "/", endpointName + "Id");
        } else {
            endpointMethodBuilder
                    .addStatement("return new $T(getEndpoint(), $L)", endpointClassName, endpointName + "Id");
        }

        return endpointMethodBuilder.build();
    }

    private static String capitalize(String endpoint) {
        return endpoint.substring(0, 1).toUpperCase(Locale.US) + endpoint.substring(1);
    }

    private static String underscoreIfJavaKeyword(String string) {
        for (String keyword : JAVA_KEYWORDS) {
            if (string.equals(keyword)) {
                return string + "_";
            }
        }
        return string;
    }
}
