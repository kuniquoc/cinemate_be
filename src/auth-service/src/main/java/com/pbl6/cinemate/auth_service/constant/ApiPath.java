package com.pbl6.cinemate.auth_service.constant;

public final class ApiPath {
    //    auth
    public static final String API = "/api/v1";
    public static final String AUTH = API + "/auth";
    //       roles
    public static final String ROLES = AUTH + "/roles";
    //    permissions
    public static final String PERMISSIONS = AUTH + "/permissions";
    //    accounts
    public static final String ACCOUNTS = AUTH + "/accounts";
    //    devices
    public static final String DEVICES = AUTH + "/devices";
    //    admin devices
    public static final String ADMIN_DEVICES = AUTH + "/admin/devices";
    public static final String VERIFY_EMAIL = API + "/verify-email";
    public static final String VERIFY_TOKEN = API + "/verify-token";
    public static final String SIGN_UP = API + "/sign-up";
    public static final String VERIFY_ACCOUNT = API + "/verify-account";
    public static final String LOGIN = API + "/login";
    public static final String FORGOT_PASSWORD = API + "/forgot-password";
    public static final String VERIFY_OTP = API + "/verify-otp";
    public static final String RESET_PASSWORD = API + "/reset-password";
    public static final String LOGOUT = API + "/log-out";
    public static final String CHANGE_PASSWORD = API + "/change-password";
    public static final String REFRESH_TOKEN = API + "/refresh-token";
    public static final String VERIFY_JWT = API + "/verify-jwt";

    private ApiPath() {
    }
}
