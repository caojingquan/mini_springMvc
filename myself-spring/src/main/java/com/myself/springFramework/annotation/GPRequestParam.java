package com.myself.springFramework.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface GPRequestParam {
    String value() default "";
}