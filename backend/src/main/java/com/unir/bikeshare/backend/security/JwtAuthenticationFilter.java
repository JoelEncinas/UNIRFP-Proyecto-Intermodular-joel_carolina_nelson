package com.unir.bikeshare.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final CustomUserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    final String token = authHeader.substring(7);
    final Long userId;

    try {
      userId = jwtService.extractUserId(token);
    } catch (Exception e) {
      filterChain.doFilter(request, response);
      return;
    }

    if (userId == null) {
      filterChain.doFilter(request, response);
      return;
    }

    if (SecurityContextHolder.getContext().getAuthentication() == null) {

      final UserDetails userDetails;
      try {
        userDetails = userDetailsService.loadUserById(userId);
      } catch (UsernameNotFoundException ex) {
        filterChain.doFilter(request, response);
        return;
      }

      if (jwtService.isTokenValid(token)) {

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities());

        authToken.setDetails(
            new WebAuthenticationDetailsSource()
                .buildDetails(request));

        SecurityContextHolder.getContext()
            .setAuthentication(authToken);
      }
    }

    filterChain.doFilter(request, response);
  }
}
