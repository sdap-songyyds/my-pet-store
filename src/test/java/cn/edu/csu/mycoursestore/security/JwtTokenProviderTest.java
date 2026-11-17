package cn.edu.csu.mycoursestore.security;

import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import cn.edu.csu.mycoursestore.user.entity.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-0123456789-abcdefghij";

    private final JwtTokenProvider provider = new JwtTokenProvider(
            new JwtProperties(SECRET, Duration.ofHours(2), "mycoursestore", "AUTH_TOKEN", false));

    private StoreUser user() {
        StoreUser user = new StoreUser();
        user.setId(42L);
        user.setRole(UserRole.CUSTOMER);
        return user;
    }

    @Test
    void issuedTokenParsesBackToUserId() {
        assertEquals(Optional.of(42L), provider.parseUserId(provider.issue(user())));
    }

    @Test
    void tamperedSignatureIsRejected() {
        String[] parts = provider.issue(user()).split("\\.");
        char first = parts[2].charAt(0);
        parts[2] = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);

        assertTrue(provider.parseUserId(String.join(".", parts)).isEmpty());
    }

    @Test
    void tokenFromDifferentIssuerIsRejected() {
        JwtTokenProvider other = new JwtTokenProvider(
                new JwtProperties(SECRET, Duration.ofHours(2), "other-issuer", "AUTH_TOKEN", false));

        assertTrue(provider.parseUserId(other.issue(user())).isEmpty());
    }

    @Test
    void malformedTokenIsRejected() {
        assertTrue(provider.parseUserId("not-a-jwt").isEmpty());
    }

    @Test
    void shortSecretIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new JwtProperties("too-short", Duration.ofHours(2), "mycoursestore", "AUTH_TOKEN", false));
    }
}