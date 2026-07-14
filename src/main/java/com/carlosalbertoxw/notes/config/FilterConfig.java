package com.carlosalbertoxw.notes.config;

import com.carlosalbertoxw.notes.auth.ApiKeyAuthFilter;
import com.carlosalbertoxw.notes.auth.ApiKeyRepository;
import com.carlosalbertoxw.notes.web.HttpsEnforcementFilter;
import com.carlosalbertoxw.notes.web.ProblemResponder;
import com.carlosalbertoxw.notes.web.RateLimitFilter;
import com.carlosalbertoxw.notes.web.RateLimitProperties;
import com.carlosalbertoxw.notes.web.RequestSizeLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registra los filtros en orden explicito:
 * <ol>
 *   <li>HTTPS/HSTS (endurecimiento de transporte, opcional por configuracion),</li>
 *   <li>limite de tamano del cuerpo,</li>
 *   <li>rate limiting (protege incluso la autenticacion),</li>
 *   <li>validacion de la API key (fail-closed).</li>
 * </ol>
 */
@Configuration(proxyBeanMethods = false)
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<HttpsEnforcementFilter> httpsEnforcementFilter(
            @Value("${app.security.https-enforced:true}") boolean httpsEnforced) {
        var registration = new FilterRegistrationBean<>(new HttpsEnforcementFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        registration.setEnabled(httpsEnforced);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestSizeLimitFilter> requestSizeLimitFilter(
            @Value("${app.max-request-body-bytes:1048576}") long maxBytes, ProblemResponder problems) {
        var registration = new FilterRegistrationBean<>(new RequestSizeLimitFilter(maxBytes, problems));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            RateLimitProperties properties, ProblemResponder problems) {
        var registration = new FilterRegistrationBean<>(
                new RateLimitFilter(properties.permitLimit(), properties.windowSeconds(), problems));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilter(
            ApiKeyRepository apiKeys, ProblemResponder problems) {
        var registration = new FilterRegistrationBean<>(new ApiKeyAuthFilter(apiKeys, problems));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
