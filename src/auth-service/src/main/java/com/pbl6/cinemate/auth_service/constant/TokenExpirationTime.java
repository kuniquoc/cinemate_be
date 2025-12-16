package com.pbl6.cinemate.auth_service.constant;

public final class TokenExpirationTime {
    public static final int DELETE_ACCOUNT_TOKEN_MINUTES = 5;
    public static final int VERIFY_ACCOUNT_TOKEN_HOURS = 24;
    public static final int RESET_PASSWORD_TOKEN_MINUTES = 5;

    private TokenExpirationTime() {
    }
}
