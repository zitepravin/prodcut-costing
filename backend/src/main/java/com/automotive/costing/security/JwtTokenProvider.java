package com.automotive.costing.security;

import com.automotive.costing.config.JwtProperties;
import com.automotive.costing.platform.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Issues and validates JWTs (SEC-02 baseline auth; SSO/SAML/OAuth2 per TDD 8.1
 * layers on top of this in a later increment without replacing it).
 *
 * SEC-08 ("deactivate a user account immediately, revoking all active sessions") is
 * implemented via a {@code tokenVersion} claim rather than a server-side token blocklist
 * store: every token embeds the value of {@code User.tokenVersion} at issuance time, and
 * is only considered valid if that value still matches the user's *current*
 * {@code tokenVersion} at validation time. Deactivating a user (or changing their
 * password) increments {@code tokenVersion}, which instantly invalidates every
 * previously-issued token - access AND refresh - without needing a separate revocation
 * list to check on every request. This trades a small amount of claim-embedding
 * complexity for not needing Redis/a blocklist table just for logout-everywhere, which
 * felt like the right trade for this system's scale; flagged as a design choice, not the
 * only valid approach, in case the client's actual session-management expectations differ.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_AUTHORITIES = "authorities";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String CLAIM_TOKEN_VERSION = "tokenVersion";

    private final JwtProperties jwtProperties;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    
    public static final String TYPE_PORTAL = "PORTAL";
    private static final String CLAIM_SUPPLIER_ID = "supplierId";
    private static final String CLAIM_PORTAL_ACCOUNT_ID = "portalAccountId";

    public String generatePortalToken(String email, String supplierId, String portalAccountId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpirationMs());
        return Jwts.builder()
                .subject(email)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiry)
                .claim(CLAIM_TOKEN_TYPE, TYPE_PORTAL)
                .claim(CLAIM_AUTHORITIES, List.of("ROLE_SUPPLIER_PORTAL"))
                .claim(CLAIM_SUPPLIER_ID, supplierId)
                .claim(CLAIM_PORTAL_ACCOUNT_ID, portalAccountId)
                .signWith(signingKey())
                .compact();
    }

    public boolean isPortalToken(String token) {
        try {
            return TYPE_PORTAL.equals(parseClaims(token).get(CLAIM_TOKEN_TYPE));
        } catch (Exception e) {
            return false;
        }
    }

    public String extractSupplierId(String token) {
        return parseClaims(token).get(CLAIM_SUPPLIER_ID, String.class);
    }

    public String extractPortalAccountId(String token) {
        return parseClaims(token).get(CLAIM_PORTAL_ACCOUNT_ID, String.class);
    }

    public boolean isPortalTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return TYPE_PORTAL.equals(claims.get(CLAIM_TOKEN_TYPE)) && !isTokenExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtProperties.getAccessTokenExpirationMs(), "ACCESS");
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtProperties.getRefreshTokenExpirationMs(), "REFRESH");
    }

    private String buildToken(UserDetails userDetails, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        int tokenVersion = userDetails instanceof User user ? user.getTokenVersion() : 0;

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiry)
                .claim(CLAIM_AUTHORITIES, authorities)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .signWith(signingKey())
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        return (List<String>) parseClaims(token).get(CLAIM_AUTHORITIES);
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(parseClaims(token).get(CLAIM_TOKEN_TYPE));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            if (!claims.getSubject().equals(userDetails.getUsername()) || isTokenExpired(claims)) {
                return false;
            }
            if (userDetails instanceof User user) {
                Integer claimedVersion = claims.get(CLAIM_TOKEN_VERSION, Integer.class);
                int currentVersion = user.getTokenVersion();
                // A token minted before this claim existed (claimedVersion == null) is
                // treated as version 0 for comparison, matching buildToken's fallback above.
                if ((claimedVersion == null ? 0 : claimedVersion) != currentVersion) {
                    return false;
                }
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

