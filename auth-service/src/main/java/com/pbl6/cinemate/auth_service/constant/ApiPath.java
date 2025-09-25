package com.pbl6.cinemate.auth_service.constant;

public final class ApiPath {
    //    auth
    public static final String API = "/api/v1";
    public static final String SIGN_UP = API + "/sign-up";
    public static final String VERIFY_ACCOUNT = API + "/verify-account";
    public static final String LOGIN = API + "/login";
    public static final String FORGOT_PASSWORD = API + "/forgot-password";
    public static final String VERIFY_OTP = API + "/verify-otp";
    public static final String RESET_PASSWORD = API + "/reset-password";
    public static final String LOGOUT = API + "/log-out";
    public static final String CHANGE_PASSWORD = API + "/change-password";

    //       roles
    public static final String ROLES = API + "/roles";

    //    permissions
    public static final String PERMISSIONS = API + "/permissions";

    private ApiPath() {
    }
}
