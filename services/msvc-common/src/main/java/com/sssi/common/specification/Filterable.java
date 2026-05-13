package com.sssi.common.specification;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Filterable {
    FilterType type() default FilterType.TEXT;
    String[] nestedPaths() default {};
}
