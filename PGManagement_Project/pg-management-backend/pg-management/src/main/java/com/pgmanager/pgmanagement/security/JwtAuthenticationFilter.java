package com.pgmanager.pgmanagement.security;

import com.pgmanager.pgmanagement.entity.MaintenanceStaff;
import com.pgmanager.pgmanagement.entity.Owner;
import com.pgmanager.pgmanagement.entity.OwnerStatus;
import com.pgmanager.pgmanagement.repository.MaintenanceStaffRepository;
import com.pgmanager.pgmanagement.repository.OwnerRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final OwnerRepository ownerRepository;
    private final MaintenanceStaffRepository staffRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        return path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/forgot-password")
                || path.equals("/api/v1/auth/verify-otp")
                || path.equals("/api/v1/auth/reset-password")
                || path.equals("/api/v1/maintenance/auth/login")
                || path.equals("/api/v1/maintenance/auth/register");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(7).trim();

            if (token.isEmpty() || !jwtService.isTokenValid(token)) {
                reject(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid or expired token"
                );
                return;
            }

            String email = jwtService.extractEmail(token);
            String rawRole = jwtService.extractRole(token);
            String role = normalizeRole(rawRole);
            long tokenVersion = jwtService.extractSessionVersion(token);

            if (email == null || email.isBlank() || role == null) {
                reject(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid token identity or role"
                );
                return;
            }

            if (Role.SUPER_MAINTENANCE.name().equals(role)) {

                MaintenanceStaff staff = staffRepository
                        .findByEmail(email)
                        .orElse(null);

                if (staff == null
                        || !"ACTIVE".equalsIgnoreCase(
                        String.valueOf(staff.getStatus()))
                        || staff.getSessionVersion() != tokenVersion) {

                    reject(
                            response,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Maintenance authentication failed"
                    );
                    return;
                }

            } else if (Role.OWNER.name().equals(role)) {

                Owner owner = ownerRepository
                        .findByEmail(email)
                        .orElse(null);

                if (owner == null
                        || owner.getStatus() != OwnerStatus.ACTIVE
                        || owner.getRole() != Role.OWNER
                        || owner.getSessionVersion() != tokenVersion) {

                    reject(
                            response,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Owner authentication failed"
                    );
                    return;
                }

            } else {
                reject(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "Unsupported account role"
                );
                return;
            }

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority("ROLE_" + role)
                            )
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (Exception exception) {
            SecurityContextHolder.clearContext();

            // Log the exception on the server for diagnosis.
            logger.error("JWT authentication failed", exception);

            reject(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Authentication failed"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return null;
        }

        String role = rawRole.trim().toUpperCase(Locale.ROOT);

        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        return role;
    }

    private void reject(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        SecurityContextHolder.clearContext();
        response.sendError(status, message);
    }
}