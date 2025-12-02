package com.pbl6.cinemate.shared.constants;

public final class ErrorMessage {
    //common
    public static final String WRONG_FORMAT = "wrong_format";
    public static final String INTERNAL_SERVER_ERROR = "internal_server_error";

    //Sign up
    public static final String PASSWORD_CONFIRM_PASSWORD_NOT_MATCHED = "password_confirm_password_not_matched";
    public static final String TOKEN_NOT_BELONGS_TO_EMAIL = "token_not_belongs_to_email";

    //Role
    public static final String ROLE_NOT_FOUND = "role_not_found";

    //User
    public static final String USER_NOT_FOUND = "user_not_found";
    public static final String USER_ALREADY_EXISTED = "user_already_existed";

    //    AUTH
    public static final String UNAUTHORIZED = "unauthorized";
    public static final String UNAUTHENTICATED = "unauthenticated";
    public static final String INVALID_EMAIL_OR_PASSWORD = "invalid_email_or_password";
    public static final String NEW_PASSWORD_NOT_MATCHED = "new_password_not_matched";
    public static final String OLD_PASSWORD_NOT_MATCHED = "old_password_not_matched";

    //    TOKEN
    public static final String TOKEN_NOT_FOUND = "token_not_found";
    public static final String INVALID_OTP = "invalid_otp";
    public static final String EXPIRED_OTP = "expired_otp";

    //    JWT
    public static final String EXPIRED_ACCESS_TOKEN = "expired_access_token";
    public static final String EXPIRED_REFRESH_TOKEN = "expired_refresh_token";
    public static final String INVALID_ACCESS_TOKEN = "invalid_access_token";
    public static final String INVALID_REFRESH_TOKEN = "invalid_refresh_token";
    public static final String MISSING_JWT = "missing_jwt";


    //    STATE
    public static final String ACCOUNT_ALREADY_ACTIVE = "account_already_active";
    public static final String ACCOUNT_NOT_EXISTED = "account_not_existed";
    public static final String ACCOUNT_LOCKED = "account_locked";
    public static final String ACCOUNT_NOT_ACTIVE = "account_not_active";

    //    ROLE
    public static final String ROLE_ALREADY_EXISTED = "role_already_existed";

    //Permission
    public static final String PERMISSION_NOT_FOUND = "permission_not_found";
    public static final String PERMISSION_NAME_EXISTED = "permission_name_existed";

    //    ACCOUNT
    public static final String ACCOUNT_ALREADY_BANNED = "account_already_banned";
    public static final String ACCOUNT_ALREADY_UNLOCKED = "account_already_unlocked";
    public static final String CANNOT_BAN_OWN_ACCOUNT = "cannot_ban_own_account";
    public static final String CANNOT_DELETE_OWN_ACCOUNT = "cannot_delete_own_account";

    //    DEVICE
    public static final String DEVICE_NOT_FOUND = "device_not_found";

    private ErrorMessage() {
    }
}
