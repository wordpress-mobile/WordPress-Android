package org.wordpress.android.fluxc.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.AnnotationConfig;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.ActionBuilder;
import org.wordpress.android.fluxc.annotations.action.NoPayload;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import static java.util.Collections.singleton;
import static javax.lang.model.SourceVersion.latestSupported;

@SuppressWarnings("unused")
@SupportedAnnotationTypes("org.wordpress.android.fluxc.annotations.ActionEnum")
@AutoService(Processor.class)
public class ActionProcessor extends AbstractProcessor {
    private Filer mFiler;
    private Messager mMessager;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mFiler = processingEnv.getFiler();
        mMessager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return singleton(ActionEnum.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element actionElement : roundEnv.getElementsAnnotatedWith(ActionEnum.class)) {
            AnnotatedActionEnum annotatedActionEnum = new AnnotatedActionEnum(actionElement);
            createActionBuilderClass(actionElement, annotatedActionEnum);
        }

        return true;
    }

    private String createActionBuilderClass(Element tableElement, AnnotatedActionEnum annotatedActionEnum) {
        String genClassName = annotatedActionEnum.getBuilderName() + "Builder";

        TypeSpec.Builder builderClassBuilder = TypeSpec.classBuilder(genClassName)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                .superclass(ActionBuilder.class);

        for (AnnotatedAction annotatedAction : annotatedActionEnum.getActions()) {
            MethodSpec method;
            ParameterizedTypeName returnType;
            boolean hasPayload =
                    !annotatedAction.getPayloadType().toString().equals(TypeName.get(NoPayload.class).toString());

            if (hasPayload) {
                // Create builder method for Action with a prescribed payload type
                returnType = ParameterizedTypeName.get(ClassName.get(Action.class),
                        TypeName.get(annotatedAction.getPayloadType()));

                method = MethodSpec.methodBuilder(CodeGenerationUtils.getActionBuilderMethodName(annotatedAction))
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(returnType)
                        .addParameter(TypeName.get(annotatedAction.getPayloadType()), "payload")
                        .addStatement("return new $T<>($T.$L, $N)", Action.class, tableElement.asType(),
                                annotatedAction.getActionName(), "payload")
                        .build();
            } else {
                // Create builder method for Action with no payload
                returnType = ParameterizedTypeName.get(Action.class, Void.class);

                method = MethodSpec.methodBuilder(CodeGenerationUtils.getActionBuilderMethodName(annotatedAction))
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(returnType)
                        .addStatement("return $L($T.$L)", "generateNoPayloadAction", tableElement.asType(),
                                annotatedAction.getActionName())
                        .build();
            }
            builderClassBuilder.addMethod(method);
        }

        TypeSpec builderClass = builderClassBuilder.build();

        JavaFile javaFile = JavaFile.builder(AnnotationConfig.PACKAGE, builderClass)
                .build();

        try {
            javaFile.writeTo(mFiler);
        } catch (IOException e) {
            mMessager.printMessage(Diagnostic.Kind.ERROR, "Failed to create file: " + e.getMessage());
        }

        return AnnotationConfig.PACKAGE + "." + genClassName;
    }
}
