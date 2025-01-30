package com.codeReview.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.codeReview.code.User;
import com.codeReview.codeService.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final UserService userService;
    private final SecretKey secretKey;

    @Autowired
    public JwtAuthenticationFilter(UserService userService, SecretKey jwtSecretKey) {
        this.userService = userService;
        this.secretKey = jwtSecretKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Optional<String> tokenOpt = extractToken(request);

        if (tokenOpt.isPresent()) {
            String token = tokenOpt.get();
            logger.info("Extracted Token: {}", token);

            try {
                // Commenting out the token validation
                // Claims claims = validateAndParseToken(token);
                // setAuthentication(claims, response); // Pass response to setAuthentication
            } catch (JwtException e) {
                logger.error("Invalid JWT Token: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid JWT Token\"}");
                return; // Stop further processing
            }
        }

        filterChain.doFilter(request, response); // Continue the filter chain
    }

    private Claims validateAndParseToken(String token) throws JwtException {
        try {
            logger.info("Validating Token: {}", token);
            logger.info("Using Secret Key: {}", Base64.getEncoder().encodeToString(secretKey.getEncoded())); // Log the secret key
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            logger.error("Error while parsing token: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    private void setAuthentication(Claims claims, HttpServletResponse response) { // Accept response as a parameter
        String email = claims.getSubject();
        User user = userService.findUserByEmail(email);

        if (user != null) {
            String role = claims.get("role", String.class);
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
            // SecurityContextHolder.getContext().setAuthentication(authentication); // Commented out the authentication line
            logger.info("User  authenticated: {} with role: {}", email, role);
        } else {
            logger.error("User  not found for email: {}", email);
            // Set unauthorized response
            try {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"User  not found\"}");
            } catch (IOException e) {
                logger.error("Error writing response: {}", e.getMessage());
            }
        }
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return Optional.of(bearerToken.substring(7)); // Extract the token
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/") ||
               path.equals("/api/register") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v 3/api-docs");
    }
}