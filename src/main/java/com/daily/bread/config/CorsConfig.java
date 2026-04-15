package com.daily.bread.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private static final List<String> DEFAULT_PATTERNS = List.of(
			"http://localhost:*",
			"http://127.0.0.1:*",
			// UI no EasyPanel (origem real do browser no login)
			"https://daily-bread-app-web-daily-bread-ui.0eeclf.easypanel.host",
			// Outros serviços no mesmo cluster EasyPanel (mesmo sufixo)
			"https://*.0eeclf.easypanel.host");

	/**
	 * Origins adicionais (produção). Separados por vírgula. Ex.:
	 * https://meu-dominio.com,https://www.meu-dominio.com
	 * Variável de ambiente: BREAD_CORS_EXTRA_ORIGIN_PATTERNS
	 */
	@Value("${bread.cors.extra-allowed-origin-patterns:}")
	private String extraAllowedOriginPatterns;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		List<String> patterns = new ArrayList<>(DEFAULT_PATTERNS);
		if (StringUtils.hasText(extraAllowedOriginPatterns)) {
			Arrays.stream(extraAllowedOriginPatterns.split(","))
					.map(String::trim)
					.filter(StringUtils::hasText)
					.forEach(patterns::add);
		}
		registry.addMapping("/**")
				.allowedOriginPatterns(patterns.toArray(String[]::new))
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.maxAge(3600);
	}
}
