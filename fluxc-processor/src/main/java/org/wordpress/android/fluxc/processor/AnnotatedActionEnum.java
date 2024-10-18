package org.wordpress.android.fluxc.processor;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;

/**
 * Blueprint for an {@link ActionEnum}-annotated enum after processing.
 */
public class AnnotatedActionEnum {
    private String mBuilderName;
    private List<AnnotatedAction> mActions = new ArrayList<>();

    public AnnotatedActionEnum(Element typeElement) {
        ActionEnum actionEnumAnnotation = typeElement.getAnnotation(ActionEnum.class);
        String userDefinedName = actionEnumAnnotation.name();
        mBuilderName = userDefinedName.equals("") ? typeElement.getSimpleName().toString() : userDefinedName;

        for (Element enumElement : typeElement.getEnclosedElements()) {
            Action actionAnnotation = enumElement.getAnnotation(Action.class);

            if (actionAnnotation == null) {
                continue;
            }
            mActions.add(new AnnotatedAction(enumElement, actionAnnotation));
        }
    }

    public String getBuilderName() {
        return mBuilderName;
    }

    public List<AnnotatedAction> getActions() {
        return Collections.unmodifiableList(mActions);
    }
}
