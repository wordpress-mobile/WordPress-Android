package org.wordpress.android.fluxc.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.wordpress.android.fluxc.annotations.Endpoint;
import org.wordpress.android.fluxc.annotations.endpoint.EndpointNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Modifier;

public class RESTPoet {
    private static final String[] JAVA_KEYWORDS = {
            "new", "abstract", "assert", "boolean",
            "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "extends", "false",
            "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native",
            "null", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true",
            "try", "void", "volatile", "while"
    };

    private static TypeName sBaseEndpointClass;
    private static Pattern sVariableEndpointPattern;

    public static TypeSpec generate(EndpointNode rootNode, String fileName, Class baseEndpointClass,
                                    Pattern variableEndpointPattern) {
        sBaseEndpointClass = ClassName.get(baseEndpointClass);
        sVariableEndpointPattern = variableEndpointPattern;

        TypeSpec.Builder wpcomRestBuilder = TypeSpec.classBuilder(fileName)
                .addModifiers(Modifier.PUBLIC);

        for (EndpointNode endpoint : rootNode.getChildren()) {
            addEndpointToBuilder(endpoint, wpcomRestBuilder);
        }

        return wpcomRestBuilder.build();
    }

    private static void addEndpointToBuilder(EndpointNode endpointNode, TypeSpec.Builder classBuilder) {
        Matcher variableEndpointMatcher = sVariableEndpointPattern.matcher(endpointNode.getLocalEndpoint());

        if (variableEndpointMatcher.find()) {
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
                        .initializer("new $T($S)", sBaseEndpointClass, "/" + endpointNode.getLocalEndpoint());
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
            List<MethodSpec> endpointMethods = generateEndpointMethods(endpointNode, sBaseEndpointClass);

            for (MethodSpec endpointMethod : endpointMethods) {
                classBuilder.addMethod(endpointMethod);
            }
        } else {
            String innerClassName;
            if (endpointNode.getParent().getCleanEndpointName().equals(endpointName)) {
                // Special rule for situations like '.../media/$media_ID/` where the inner class needs to be renamed
                innerClassName = capitalize(endpointName) + "Item" + "Endpoint";
            } else {
                innerClassName = capitalize(endpointName) + "Endpoint";
            }

            TypeSpec.Builder endpointClassBuilder = TypeSpec.classBuilder(innerClassName)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .superclass(sBaseEndpointClass);

            // Add a constructor for each type this endpoint accepts (usually long)
            for (Class endpointType : getVariableEndpointTypes(endpointNode)) {
                String variableName = endpointName;
                if (endpointType.equals(long.class) && !endpointName.toLowerCase(Locale.US).endsWith("id")) {
                    variableName = endpointName + "Id";
                }

                MethodSpec.Builder endpointConstructorBuilder = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .addParameter(String.class, "previousEndpoint")
                        .addParameter(endpointType, variableName);

                if (endpointNode.getLocalEndpoint().contains(":")) {
                    // Special case for endpoints of type '/item:$item/'
                    endpointConstructorBuilder.addStatement("super($L, $S + $L)", "previousEndpoint",
                            endpointName + ":", variableName);
                } else {
                    endpointConstructorBuilder.addStatement("super($L, $L)", "previousEndpoint", variableName);
                }
                endpointClassBuilder.addMethod(endpointConstructorBuilder.build());
            }

            TypeName endpointClassName = ClassName.get("", innerClassName);

            // Build annotated accessor method for variable endpoint
            List<MethodSpec> endpointMethods = generateEndpointMethods(endpointNode, endpointClassName);

            for (EndpointNode childEndpoint : endpointNode.getChildren()) {
                addEndpointToBuilder(childEndpoint, endpointClassBuilder);
            }

            for (MethodSpec endpointMethod : endpointMethods) {
                classBuilder.addMethod(endpointMethod);
            }

            classBuilder.addType(endpointClassBuilder.build());
        }
    }

    private static List<MethodSpec> generateEndpointMethods(EndpointNode endpointNode, TypeName endpointClassName) {
        List<MethodSpec> endpointMethods = new ArrayList<>();

        for (Class endpointType : getVariableEndpointTypes(endpointNode)) {
            String endpointName = endpointNode.getCleanEndpointName();

            String methodName = endpointName;
            if (endpointNode.getParent().getCleanEndpointName().equals(endpointName)) {
                // Special rule for situations like '.../media/$media_ID/`
                methodName = "item";
            }

            String variableName = endpointName;
            if (endpointType.equals(long.class) && !endpointName.toLowerCase(Locale.US).endsWith("id")) {
                variableName = endpointName + "Id";
            }

            MethodSpec.Builder endpointMethodBuilder = MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(endpointClassName)
                    .addParameter(endpointType, variableName)
                    .addAnnotation(AnnotationSpec.builder(Endpoint.class)
                            .addMember("value", "$S", endpointNode.getFullEndpoint())
                            .build());

            if (endpointNode.getParent().isRoot()) {
                if (endpointNode.getLocalEndpoint().contains(":") && !endpointNode.hasChildren()) {
                    // Special case for endpoints of type '/item:$item/'
                    // (the case with children is covered in the constructor, not here)
                    endpointMethodBuilder.addModifiers(Modifier.STATIC)
                            .addStatement("return new $T($S, $S + $L)", endpointClassName, "/",
                                    endpointName + ":", variableName);
                } else {
                    endpointMethodBuilder.addModifiers(Modifier.STATIC)
                            .addStatement("return new $T($S, $L)", endpointClassName, "/", variableName);
                }
            } else {
                if (endpointNode.getLocalEndpoint().contains(":") && !endpointNode.hasChildren()) {
                    // Special case for endpoints of type '/item:$item/'
                    // (the case with children is covered in the constructor, not here)
                    endpointMethodBuilder
                            .addStatement("return new $T(getEndpoint(), $S + $L)", endpointClassName,
                                    endpointName + ":", variableName);
                } else {
                    endpointMethodBuilder
                            .addStatement("return new $T(getEndpoint(), $L)", endpointClassName, variableName);
                }
            }
            endpointMethods.add(endpointMethodBuilder.build());
        }

        return endpointMethods;
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

    private static List<Class> getVariableEndpointTypes(EndpointNode endpointNode) {
        List<Class> endpointTypes = new ArrayList<>();

        for (String endpointType : endpointNode.getEndpointTypes()) {
            switch (endpointType) {
                case "String":
                    endpointTypes.add(String.class);
                    break;
                case "long":
                    endpointTypes.add(long.class);
                    break;
            }
        }

        if (endpointTypes.isEmpty()) {
            endpointTypes.add(long.class);
        }

        return endpointTypes;
    }
}
