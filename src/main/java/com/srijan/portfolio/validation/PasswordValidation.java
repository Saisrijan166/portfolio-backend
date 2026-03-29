package com.srijan.portfolio.validation;

public final class PasswordValidation {

    private PasswordValidation() {
    }

    public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,72}$";
    public static final String PASSWORD_MESSAGE =
            "Password must contain uppercase and lowercase letters, a number, and a special character";
}
