package com.carlosalbertoxw.notes.auth;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resuelve los parametros anotados con {@link CurrentKeyId} leyendo el key_id
 * que {@link ApiKeyAuthFilter} dejo en la peticion.
 */
public class CurrentKeyIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentKeyId.class)
                && parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        Object keyId = webRequest.getAttribute(ApiKeyAuthFilter.KEY_ID_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);
        if (keyId == null) {
            throw new IllegalStateException("No hay cliente autenticado en la peticion.");
        }
        return keyId;
    }
}
