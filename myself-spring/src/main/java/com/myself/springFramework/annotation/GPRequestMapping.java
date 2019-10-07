package com.myself.springFramework.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GPRequestMapping {
    String value() default "";
}
