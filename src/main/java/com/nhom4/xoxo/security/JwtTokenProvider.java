package com.nhom4.xoxo.security;

import java.security.Key;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.nhom4.xoxo.entity.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt-secret}")
    private String jwtSecret;

    @Value("${app.jwt-expiration-milliseconds}")
    private long jwtExpirationInMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(UserDetails userDetails) {
        String username = userDetails.getUsername();
        Set<Role> roles = new HashSet<>();
        userDetails.getAuthorities().forEach(auth -> {
            if (auth.getAuthority().equals("ROLE_USER"))
                roles.add(Role.USER);
            if (auth.getAuthority().equals("ROLE_OWNER"))
                roles.add(Role.OWNER);
            if (auth.getAuthority().equals("ROLE_ADMIN"))
                roles.add(Role.ADMIN);
        });

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles.stream().map(Role::name).collect(Collectors.toList()))
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateToken(Authentication authentication) {

        String username;

        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails = (org.springframework.security.core.userdetails.UserDetails) authentication
                    .getPrincipal();
            username = userDetails.getUsername();
        } else if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            org.springframework.security.oauth2.core.user.OAuth2User oauth2User = (org.springframework.security.oauth2.core.user.OAuth2User) authentication
                    .getPrincipal();
            username = oauth2User.getAttribute("email");
        } else {
            username = authentication.getName();
        }
        // get role from authentication
        Set<Role> roles = new HashSet<>();

        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER"))) {
            roles.add(Role.USER);
        }
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OWNER"))) {
            roles.add(Role.OWNER);
        }
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            roles.add(Role.ADMIN);
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles.stream().map(Role::name).collect(Collectors.toList()))
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}