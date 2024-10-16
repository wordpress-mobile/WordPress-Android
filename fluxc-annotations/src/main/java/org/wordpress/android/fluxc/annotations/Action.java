package org.wordpress.android.fluxc.annotations;

import org.wordpress.android.fluxc.annotations.action.NoPayload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Defines an individual action with optional payload. To annotate an option with no payload, don't set the
 * {@link Action#payloadType}.
 */
@Target(ElementType.FIELD)
public @interface Action {
    Class payloadType() default NoPayload.class;
}
