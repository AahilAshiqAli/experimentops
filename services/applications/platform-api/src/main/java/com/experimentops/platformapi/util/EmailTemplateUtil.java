package com.experimentops.platformapi.util;

public final class EmailTemplateUtil {
    private EmailTemplateUtil() {}

    public static String buildPasswordResetBody(String passwordResetLink) {
        return """
                A password reset was requested for your ExperimentOps account.

                Use the link below to reset your password:
                %s

                If you did not request this, you can ignore this email.
                """.formatted(passwordResetLink);
    }

    public static String buildUserInviteBody(String email, String tempPassword) {
        return """
                You have been invited to ExperimentOps.

                Use the credentials below to sign in:
                Email: %s
                Temporary password: %s

                You will be asked to reset your password after signing in.
                """.formatted(email, tempPassword);
    }
}
