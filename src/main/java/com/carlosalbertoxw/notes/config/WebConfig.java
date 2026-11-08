package com.carlosalbertoxw.notes.config;

import com.carlosalbertoxw.notes.auth.CurrentKeyIdArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el resolver que inyecta el {@code key_id} autenticado en los controladores.
 */
@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentKeyIdArgumentResolver());
    }
}
