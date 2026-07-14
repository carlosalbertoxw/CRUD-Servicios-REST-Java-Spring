package com.carlosalbertoxw.notes.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta en un metodo de controlador el {@code key_id} del cliente autenticado.
 * Toda operacion queda acotada a las notas de ese cliente.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentKeyId {
}
