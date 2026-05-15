package com.panda.config;

import com.panda.interceptor.SaTokenAdminInterceptor;
import com.panda.interceptor.SaTokenDispatcherInterceptor;
import com.panda.interceptor.SaTokenUserInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final SaTokenUserInterceptor saTokenUserInterceptor;
    private final SaTokenDispatcherInterceptor saTokenDispatcherInterceptor;
    private final SaTokenAdminInterceptor saTokenAdminInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(saTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns(
                        "/user/user/signin",
                        "/user/user/login",
                        "/user/user/verification",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/error"
                );

        registry.addInterceptor(saTokenDispatcherInterceptor)
                .addPathPatterns("/dispatcher/**")
                .excludePathPatterns(
                        "/dispatcher/user/signin",
                        "/dispatcher/user/login",
                        "/dispatcher/user/verification",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/error"
                );

        registry.addInterceptor(saTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        "/admin/log/login",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/error"
                );
    }
}
