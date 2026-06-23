package com.logmonitor.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for automatic audit logging via AOP.
 *
 * <p>Use {@link #resourceSpel()} to extract the resource identifier from method arguments,
 * or leave blank to use the default resource derived from the action type.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

    /**
     * The audit action to record.
     *
     * @return audit action type
     */
    AuditAction action();

    /**
     * Optional SpEL expression to resolve the resource identifier.
     * Examples: {@code "#serverId"}, {@code "#request.serverId"}, {@code "#serverName"}.
     *
     * @return SpEL expression or empty string
     */
    String resourceSpel() default "";
}
