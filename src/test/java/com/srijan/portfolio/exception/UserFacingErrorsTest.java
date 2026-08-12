package com.srijan.portfolio.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLTransientConnectionException;

import static org.assertj.core.api.Assertions.assertThat;

class UserFacingErrorsTest {

    /** The reported incident: the connection pool is exhausted during a sign-in. */
    @Test
    void reportsAFriendlyMessageWhenTheDatabaseIsUnreachable() {
        Throwable poolExhausted = new CannotGetJdbcConnectionException(
                "Failed to obtain JDBC Connection",
                new SQLTransientConnectionException("HikariPool-1 - Connection is not available, request timed out after 20000ms")
        );

        UserFacingErrors.SafeError safeError = UserFacingErrors.resolve(poolExhausted);

        assertThat(safeError.code()).isEqualTo("DATABASE_UNAVAILABLE");
        assertThat(safeError.message()).isEqualTo(
                "We're having trouble reaching our database right now. Please try again in a few moments.");
        assertThat(safeError.message()).doesNotContain("HikariPool", "JDBC", "timed out after");
    }

    @Test
    void classifiesTheCauseWhenSpringSecurityCopiesTheRawMessage() {
        // DaoAuthenticationProvider wraps anything the UserDetailsService throws, copying its message.
        Throwable wrapped = new InternalAuthenticationServiceException(
                "FATAL: too many connections for role \"portfolio\"",
                new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection")
        );

        assertThat(UserFacingErrors.resolve(wrapped).code()).isEqualTo("DATABASE_UNAVAILABLE");
        assertThat(UserFacingErrors.messageFor(wrapped)).doesNotContain("too many connections");
    }

    @Test
    void neverEchoesAnUnrecognisedExceptionMessage() {
        Throwable leaky = new IllegalStateException("org.hibernate.exception.JDBCConnectionException at line 42");

        UserFacingErrors.SafeError safeError = UserFacingErrors.resolve(leaky);

        assertThat(safeError.code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(safeError.message()).isEqualTo(UserFacingErrors.GENERIC_MESSAGE);
        assertThat(safeError.message()).doesNotContain("hibernate", "line 42");
    }

    @Test
    void keepsOurOwnMessagesIntact() {
        ApiException ours = new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "That username is already taken");

        UserFacingErrors.SafeError safeError = UserFacingErrors.resolve(new RuntimeException("wrapper", ours));

        assertThat(safeError.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(safeError.code()).isEqualTo("USERNAME_TAKEN");
        assertThat(safeError.message()).isEqualTo("That username is already taken");
    }

    @Test
    void distinguishesMailAndUpstreamFailures() {
        assertThat(UserFacingErrors.resolve(new MailSendException("relay refused")).code())
                .isEqualTo("EMAIL_DELIVERY_FAILED");
        assertThat(UserFacingErrors.resolve(new IOException("Failed to call Groq generation: connection reset")).code())
                .isEqualTo("UPSTREAM_UNAVAILABLE");
        assertThat(UserFacingErrors.resolve(new ConnectException("Connection refused")).code())
                .isEqualTo("UPSTREAM_UNAVAILABLE");
    }

    @Test
    void doesNotTreatARejectedWriteAsAnUnreachableDatabase() {
        assertThat(UserFacingErrors.resolve(new DataIntegrityViolationException("duplicate key")).code())
                .isEqualTo("INTERNAL_SERVER_ERROR");
        // Other data access failures still classify as a database problem.
        assertThat(UserFacingErrors.resolve(new CannotAcquireLockException("deadlock")).code())
                .isEqualTo("DATABASE_UNAVAILABLE");
    }

    @Test
    void survivesADeepOrSelfReferencingCauseChain() {
        Throwable deep = new RuntimeException("level-0");
        for (int i = 1; i < 30; i++) {
            deep = new RuntimeException("level-" + i, deep);
        }

        assertThat(UserFacingErrors.resolve(deep).message()).isEqualTo(UserFacingErrors.GENERIC_MESSAGE);
        assertThat(UserFacingErrors.resolve(null).message()).isEqualTo(UserFacingErrors.GENERIC_MESSAGE);
    }
}
