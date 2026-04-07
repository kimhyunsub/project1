package com.attendance.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(resolveSecret(jwtProperties.getSecret()));
    }

    public String generateToken(Long employeeId, String employeeCode, Long companyId, String deviceId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getAccessTokenExpirationSeconds());

        return Jwts.builder()
            .subject(employeeCode)
            .claim("employeeId", employeeId)
            .claim("companyId", companyId)
            .claim("deviceId", deviceId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String getEmployeeCode(String token) {
        return getClaims(token).getSubject();
    }

    public Long getEmployeeId(String token) {
        Object employeeId = getClaims(token).get("employeeId");
        if (employeeId instanceof Integer value) {
            return value.longValue();
        }
        if (employeeId instanceof Long value) {
            return value;
        }
        return Long.parseLong(String.valueOf(employeeId));
    }

    public String getDeviceId(String token) {
        Object deviceId = getClaims(token).get("deviceId");
        return deviceId == null ? null : String.valueOf(deviceId);
    }

    public Long getCompanyId(String token) {
        Object companyId = getClaims(token).get("companyId");
        if (companyId instanceof Integer value) {
            return value.longValue();
        }
        if (companyId instanceof Long value) {
            return value;
        }
        return Long.parseLong(String.valueOf(companyId));
    }

    public Instant getExpiration(String token) {
        return getClaims(token).getExpiration().toInstant();
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    private byte[] resolveSecret(String rawSecret) {
        try {
            return Decoders.BASE64.decode(rawSecret);
        } catch (IllegalArgumentException | DecodingException ex) {
            return Keys.hmacShaKeyFor(rawSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)).getEncoded();
        }
    }
}
