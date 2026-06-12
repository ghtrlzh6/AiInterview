package com.aiinterview.security;

import com.aiinterview.common.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtTokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (isTokenBlacklisted(token)) {
                    throw BusinessException.unauthorized("Token 已失效，请重新登录");
                }
                Claims claims = jwtUtil.parseToken(token);
                if (jwtUtil.isRefreshToken(claims)) {
                    throw BusinessException.unauthorized("无效的 Access Token");
                }
                Long userId = Long.valueOf(claims.get("userId").toString());
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);
                SecurityUser user = new SecurityUser(userId, username, role);
                var auth = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (BusinessException e) {
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.contains(token);
    }
}
