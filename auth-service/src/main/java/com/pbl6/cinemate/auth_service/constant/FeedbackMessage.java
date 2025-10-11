package com.pbl6.cinemate.auth_service.constant;

public final class FeedbackMessage {
    // Authentication
    public static final String LOGGED_IN = "Logged in successfully";
    public static final String SIGNED_UP = "Signed up successfully";
    public static final String FORGOT_PASSWORD = "Sent the OTP to your email";
    public static final String OTP_VERIFIED = "OTP verified successfully";
    public static final String RESET_PASSWORD = "Password reset successfully";
    public static final String LOGGED_OUT = "Logged out successfully";
    public static final String PASSWORD_CHANGED = "Password changed successfully";
    public static final String TOKEN_REFRESHED = "Token refreshed successfully";
    public static final String TOKEN_VERIFIED = "Token is verified";
    public static final String EMAIL_SENT = "Verify email token was sent";
    public static final String EMAIL_VERIFIED = "Email verified successfully";

    //    STATE
    public static final String ACCOUNT_VERIFIED = "Account verified successfully";

    // Permission
    public static final String PERMISSION_CREATED = "Permission created successfully";
    public static final String PERMISSION_FETCHED = "Permission fetched successfully";
    public static final String PERMISSION_UPDATED = "Permission updated successfully";
    public static final String PERMISSION_DELETED = "Permission deleted successfully";
    public static final String PERMISSIONS_FETCHED = "Permissions fetched successfully";

    //    ROLE
    public static final String ROLE_CREATED = "Role created successfully";
    public static final String ROLES_RETRIEVED = "Roles retrieved successfully";
    public static final String ROLE_RETRIEVED = "Role retrieved successfully";
    public static final String ROLE_UPDATED = "Role updated successfully";

    //    ROLE - PERMISSION
    public static final String PERMISSIONS_ADDED_TO_ROLE = "Permissions added to role successfully";
    public static final String PERMISSIONS_FETCHED_FOR_ROLE = "Permissions fetched for role";
    public static final String PERMISSION_REMOVED_FROM_ROLE = "Permission removed from role successfully";

    private FeedbackMessage() {
    }
}
