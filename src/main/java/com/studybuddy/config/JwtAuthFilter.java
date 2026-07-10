package com.studybuddy.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println("DEBUG JwtAuthFilter: Authorization header = " + authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            boolean valid = jwtUtil.isTokenValid(token);
            System.out.println("DEBUG JwtAuthFilter: token valid = " + valid);

            if (valid) {
                String email = jwtUtil.extractEmail(token);
                Long userId = jwtUtil.extractUserId(token);
                System.out.println("DEBUG JwtAuthFilter: authenticated userId=" + userId + " email=" + email);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                authToken.setDetails(email);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } else {
            System.out.println("DEBUG JwtAuthFilter: no Bearer header found");
        }

        filterChain.doFilter(request, response);

        System.out.println("DEBUG JwtAuthFilter: after chain, authentication in context = "
                + SecurityContextHolder.getContext().getAuthentication());
    }
}