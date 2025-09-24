package com.pbl6.cinemate.auth_service.constant;

public final class FeedbackMessage {
    // Authentication
    public static final String LOGGED_IN = "Logged in successfully";
    public static final String SIGNED_UP = "Signed up successfully";
    public static final String FORGOT_PASSWORD = "Sent the OTP to your email";
    public static final String OTP_VERIFIED = "OTP verified successfully";
    public static final String RESET_PASSWORD = "Password reset successfully";

    //    STATE
    public static final String ACCOUNT_VERIFIED = "Account verified successfully";

    // Permission
    public static final String PERMISSION_CREATED = "Permission created successfully";
    public static final String PERMISSION_FETCHED = "Permission fetched successfully";
    public static final String PERMISSION_UPDATED = "Permission updated successfully";
    public static final String PERMISSION_DELETED = "Permission deleted successfully";
    public static final String PERMISSIONS_FETCHED = "Permissions fetched successfully";

    private FeedbackMessage() {
    }
}
