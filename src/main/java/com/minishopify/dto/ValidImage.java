package com.minishopify.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Checks that an uploaded file is JPEG, PNG, GIF or WebP image
@Documented
@Constraint(validatedBy = ImageFileValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidImage {

    String message() default "Image must be a JPEG, PNG, GIF or WebP file of at most 2 MB";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
