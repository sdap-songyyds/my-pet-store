package cn.edu.csu.mycoursestore.user.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserProfile user
) {

    public AuthResponse(String token, long expiresInSeconds, UserProfile user) {
        this(token, "Bearer", expiresInSeconds, user);
    }
}