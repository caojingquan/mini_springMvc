package com.myself.springFramework.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GPAutowired {
    String value() default "";
}
