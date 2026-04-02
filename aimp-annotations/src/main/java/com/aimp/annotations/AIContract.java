package com.aimp.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a persisted generation version for an AI-implemented contract type.
 *
 * <p>AIMP uses the contract fully qualified name together with this version to
 * look up previously stored generated implementations.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AIContract {
    /**
     * Returns the explicit persisted implementation version for this contract.
     *
     * @return the contract version used for persisted implementation lookup
     */
    String version() default "1";
}
