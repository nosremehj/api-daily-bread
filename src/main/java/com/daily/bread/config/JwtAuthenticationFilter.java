package com.daily.bread.config;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.daily.bread.auth.services.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		String token = header.substring(7);
		if (!jwtService.isValid(token)) {
			if (isPublicAuthPath(request.getRequestURI())) {
				filterChain.doFilter(request, response);
				return;
			}
			response.sendError(HttpStatus.UNAUTHORIZED.value());
			return;
		}
		String username = jwtService.extractUsername(token);
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
				username, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
		auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(auth);
		filterChain.doFilter(request, response);
	}

	private static boolean isPublicAuthPath(String uri) {
		return uri.startsWith("/api/v1/auth/register")
				|| uri.startsWith("/api/v1/auth/login")
				|| uri.startsWith("/api/v1/auth/refresh")
				|| uri.startsWith("/api/v1/bible");
	}
}
