package com.proseg.msvc_auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionsTest {
    @Test
    void factoriesExposeStableCodesAndStatuses() {
        assertThat(AuthenticationException.invalidToken().getErrorCode()).isEqualTo("INVALID_TOKEN");
        assertThat(AuthenticationException.invalidCredentials().getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(AuthenticationException.tokenExpired().getErrorCode()).isEqualTo("TOKEN_EXPIRED");
        assertThat(AuthenticationException.accountDisabled().getErrorCode()).isEqualTo("ACCOUNT_DISABLED");
        assertThat(AuthenticationException.accountLocked().getErrorCode()).isEqualTo("ACCOUNT_LOCKED");
        assertThat(AuthorizationException.insufficientPermissions().getErrorCode()).isEqualTo("INSUFFICIENT_PERMISSIONS");
        assertThat(AuthorizationException.roleNotAllowed("ROLE").getErrorCode()).isEqualTo("ROLE_NOT_ALLOWED");
        assertThat(AuthorizationException.accountNotApproved("PENDING").getErrorCode()).isEqualTo("ACCOUNT_NOT_APPROVED");
        assertThat(InvitationException.invalidToken().getErrorCode()).isEqualTo("INVITATION_TOKEN_INVALID");
        assertThat(InvitationException.expiredToken().getErrorCode()).isEqualTo("INVITATION_TOKEN_EXPIRED");
        assertThat(InvitationException.alreadyUsed().getErrorCode()).isEqualTo("INVITATION_TOKEN_ALREADY_USED");
        assertThat(InvitationException.userNotFound().getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(KeycloakException.roleNotFound("ROLE").getErrorCode()).isEqualTo("ROLE_NOT_FOUND");
        assertThat(KeycloakException.userNotFound("user").getErrorCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(KeycloakException.assignmentError("conflict").getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(KeycloakException.requiresAssociatedCompany().getErrorCode())
                .isEqualTo("ROLE_REQUIRES_ASSOCIATED_COMPANY");
        assertThat(KeycloakException.generic("error").getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(PasswordChangeException.confirmationMismatch().getErrorCode())
                .isEqualTo("PASSWORD_CONFIRMATION_MISMATCH");
        assertThat(PasswordChangeException.sameAsCurrent().getErrorCode()).isEqualTo("PASSWORD_REUSE_NOT_ALLOWED");
        assertThat(PasswordExpiredException.expired().getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(PasswordResetException.invalidToken().getErrorCode()).isEqualTo("PASSWORD_RESET_TOKEN_INVALID");
        assertThat(PasswordResetException.expiredToken().getErrorCode()).isEqualTo("PASSWORD_RESET_TOKEN_EXPIRED");
        assertThat(PasswordResetException.emailNotFound("user@example.test").getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(PasswordResetException.tooManyRequests().getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(TokenException.malformed().getErrorCode()).isEqualTo("TOKEN_MALFORMED");
        assertThat(TokenException.refreshTokenExpired().getErrorCode()).isEqualTo("REFRESH_TOKEN_EXPIRED");
        assertThat(TokenException.refreshTokenNotFound().getErrorCode()).isEqualTo("REFRESH_TOKEN_NOT_FOUND");
        assertThat(TokenException.signatureInvalid().getErrorCode()).isEqualTo("TOKEN_SIGNATURE_INVALID");
        assertThat(TokenException.notFound().getErrorCode()).isEqualTo("TOKEN_NOT_FOUND");
        assertThat(UserException.notFound("user").getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(UserException.userAlreadyExists("user").getErrorCode()).isEqualTo("USERNAME_ALREADY_EXISTS");
        assertThat(UserException.emailAlreadyExists("mail").getErrorCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
        assertThat(UserException.userOrEmailAlreadyExists("user", "mail").getErrorCode())
                .isEqualTo("USER_OR_EMAIL_ALREADY_EXISTS");
        assertThat(UserException.passwordMismatch().getErrorCode()).isEqualTo("PASSWORD_MISMATCH");
        assertThat(UserException.weakPassword().getErrorCode()).isEqualTo("WEAK_PASSWORD");
        assertThat(UserException.invalidUserIdFormat("bad").getErrorCode()).isEqualTo("INVALID_USER_ID");
    }

    @Test
    void exceptionHandlerMapsDomainSpringAndGenericFailures() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        assertThat(handler.handleBase(TokenException.notFound()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "missing")).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleGeneric(new ResponseStatusException(HttpStatus.CONFLICT, "conflict")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handleGeneric(new AnnotatedFailure()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleGeneric(new IllegalStateException("unexpected")).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    private static class AnnotatedFailure extends RuntimeException {
    }
}
