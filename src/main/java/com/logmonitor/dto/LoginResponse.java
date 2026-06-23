package com.logmonitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload containing a JWT access token.
 */
@Schema(description = "Authentication response with JWT access token")
public class LoginResponse {

    @Schema(description = "JWT bearer access token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
