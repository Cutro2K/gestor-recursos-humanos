package com.deso.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida que la colección de ítems de opción tenga órdenes consecutivos 1..N sin huecos ni repetidos
 * y que no repita descripciones ignorando espacios extremos y mayúsculas/minúsculas.
 */
@Documented
@Constraint(validatedBy = ItemsOpcionValidosValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ItemsOpcionValidos {

    String message() default "Los ítems de opción no cumplen las reglas de orden consecutivo o unicidad de descripción.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
