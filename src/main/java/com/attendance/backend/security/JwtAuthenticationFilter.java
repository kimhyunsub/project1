package com.attendance.backend.security;

import com.attendance.backend.domain.entity.EmployeeRole;
import com.attendance.backend.exception.ErrorResponse;
import com.attendance.backend.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
        JwtTokenProvider jwtTokenProvider,
        CustomUserDetailsService customUserDetailsService,
        ObjectMapper objectMapper
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            CustomUserDetails userDetails =
                (CustomUserDetails) customUserDetailsService.loadUserByUsername(jwtTokenProvider.getEmployeeCode(token));

            if (!userDetails.isEnabled()) {
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            if (requiresPasswordChange(userDetails) && !isPasswordChangeAllowedPath(request)) {
                writePasswordChangeRequiredResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresPasswordChange(CustomUserDetails userDetails) {
        return userDetails.getRole() == EmployeeRole.EMPLOYEE && userDetails.isPasswordChangeRequired();
    }

    private boolean isPasswordChangeAllowedPath(HttpServletRequest request) {
        return "/api/auth/change-password".equals(request.getRequestURI());
    }

    private void writePasswordChangeRequiredResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(
            response.getWriter(),
            new ErrorResponse(HttpServletResponse.SC_FORBIDDEN, "Forbidden", "비밀번호를 먼저 변경해 주세요.")
        );
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
