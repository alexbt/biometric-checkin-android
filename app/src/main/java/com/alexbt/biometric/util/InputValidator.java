package com.alexbt.biometric.util;

public class InputValidator {
    private static final String EMAIL_PATTERN = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    public static boolean isMemberValid(String firstName, String lastName, String email, String phone) {
        return (email.trim().matches(EMAIL_PATTERN) || email.trim().isEmpty())
                && (phone.trim().length() == 10 || phone.trim().isEmpty())
                && !firstName.trim().isEmpty()
                && !lastName.trim().isEmpty();
    }
}
