package edu.uclm.esi.tysweb.bealquileres;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.Arrays;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final SessionAuthorizationInterceptor sessionAuthorizationInterceptor;
    private final String frontendUrl;

    public WebConfiguration(
            @Value("${app.users.url:http://localhost:8080}") String usersServiceUrl,
            @Value("${app.frontend.url:http://localhost:4200}") String frontendUrl) {
        this.sessionAuthorizationInterceptor = new SessionAuthorizationInterceptor(usersServiceUrl);
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.sessionAuthorizationInterceptor)
                .addPathPatterns("/api/**", "/mapa/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(this.frontendUrl)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}

class SessionAuthorizationInterceptor implements HandlerInterceptor {
    private final RestClient restClient;

    SessionAuthorizationInterceptor(String usersServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(usersServiceUrl).build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String sessionToken = request.getHeader("X-Session-Token");
        if (sessionToken == null || sessionToken.isBlank()) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                sessionToken = Arrays.stream(cookies)
                        .filter(cookie -> "sessionToken".equals(cookie.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);
            }
        }

        if (sessionToken == null || sessionToken.isBlank()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Inicia sesión para continuar");
            return false;
        }

        try {
            this.restClient.get()
                    .uri("/users/validateSession")
                    .header("X-Session-Token", sessionToken)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.Unauthorized exception) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "La sesión no es válida o ha caducado");
            return false;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo validar la sesión con el servicio de usuarios",
                    exception);
        }
    }
}
