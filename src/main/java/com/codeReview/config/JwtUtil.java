package com.codeReview.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class); // Initialize logger

    @Value("${jwt.secret}")
    private String secretKey; // Inject the secret key from application properties

    private final long expirationTime = 1000 * 60 * 60 * 10; // 10 hours

    // Generate JWT token with email and role
    public String generateToken(String email, String role) {
        // String token = Jwts.builder()
        //         .setSubject(email)
        //         .claim("role", role)
        //         .setIssuedAt(new Date(System.currentTimeMillis()))
        //         .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
        //         .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        //         .compact();
        
        // logger.info("Generated Token: {}", token); // Log the generated token
        // return token;
        return null; // Return null or handle as needed
    }

    // Validate the JWT token with email check
    public boolean validateToken(String token, String email) {
        try {
            // Claims claims = Jwts.parserBuilder()
            //         .setSigningKey(getSigningKey()) // Use the signing key for validation
            //         .build()
            //         .parseClaimsJws(token)  // Pass the token
            //         .getBody();

            // logger.info("Token Validated. Claims: {}", claims);
            // String extractedEmail = getSubjectFromToken(token);
            // return (extractedEmail.equals(email) && !isTokenExpired(token));
            return false; // Return false or handle as needed
        } catch (JwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    // Get the expiration status of the token
    private boolean isTokenExpired(String token) {
        try {
            // Date expiration = getClaimsFromToken(token).getExpiration();
            // return expiration.before(new Date());
            return true; // Return true or handle as needed
        } catch (JwtException e) {
            logger.error("Error parsing token expiration: {}", e.getMessage());
            return true; // Treat as expired if parsing fails
        }
    }

    // Extract the subject (email) from the token
    private String getSubjectFromToken(String token) {
        // return getClaimsFromToken(token).getSubject();
        return null; // Return null or handle as needed
    }

    // Extract claims from the token
    private Claims getClaimsFromToken(String token) {
        // return Jwts.parserBuilder()
        //         .setSigningKey(getSigningKey())
        //         .build()
        //         .parseClaimsJws(token)
        //         .getBody();
        return null; // Return null or handle as needed
    }

    // Helper method to get the signing key
    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Expose the secret key for testing or other purposes
    public String getSecretKey() {
        return secretKey;
    }
}