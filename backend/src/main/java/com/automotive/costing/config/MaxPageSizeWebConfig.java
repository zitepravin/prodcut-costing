package com.automotive.costing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Prevents unbounded page sizes on list APIs (max 100).
 */
@Configuration
public class MaxPageSizeWebConfig implements WebMvcConfigurer, PageableHandlerMethodArgumentResolverCustomizer {

    public static final int MAX_PAGE_SIZE = 100;

    @Override
    public void customize(PageableHandlerMethodArgumentResolver pageableResolver) {
        pageableResolver.setMaxPageSize(MAX_PAGE_SIZE);
        pageableResolver.setFallbackPageable(PageRequest.of(0, 20));
    }
}
