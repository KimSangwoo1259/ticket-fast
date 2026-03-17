package com.ticket.fast.common.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long accessTokenExpiresIn
) {
    public static TokenResponse of(String accessToken, String refreshToken,  Long accessTokenExpiresIn){
        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                accessTokenExpiresIn
        );
    }
}
