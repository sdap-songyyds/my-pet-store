package cn.edu.csu.mycoursestore.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        Duration expiration,
        String issuer,
        String cookieName,
        boolean cookieSecure
) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret 至少需要 32 字节");
        }
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("app.jwt.expiration 必须为正时长");
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "mycoursestore";
        }
        if (cookieName == null || cookieName.isBlank()) {
            cookieName = "AUTH_TOKEN";
        }
    }
}