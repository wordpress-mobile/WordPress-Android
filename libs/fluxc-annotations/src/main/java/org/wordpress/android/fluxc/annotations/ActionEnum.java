package org.wordpress.android.fluxc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Defines an enum of actions related to a particular store.
 */
@Target(value = ElementType.TYPE)
public @interface ActionEnum {
    String name() default "";
}
