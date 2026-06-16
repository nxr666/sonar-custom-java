package org.springframework.transaction.annotation;

public @interface Transactional {
    boolean readOnly() default false;
    Class<?>[] rollbackFor() default {};
}
