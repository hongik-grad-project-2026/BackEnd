package com.mulmi.backend.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void revokedTokenIsNotAuthenticated() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        TokenBlacklist tokenBlacklist = mock(TokenBlacklist.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        given(request.getHeader("Authorization")).willReturn("Bearer revoked-token");
        given(tokenBlacklist.isRevoked("revoked-token")).willReturn(true);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, tokenBlacklist);
        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }
}
