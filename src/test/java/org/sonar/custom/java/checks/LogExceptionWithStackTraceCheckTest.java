package org.sonar.custom.java.checks;

import org.sonar.custom.java.checks.reliability.LogExceptionWithStackTraceCheck;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class LogExceptionWithStackTraceCheckTest {

    private static final String TEST_FILE_WITH_ISSUES = "src/test/resources/checks/LogExceptionWithStackTraceCheck.java";

    @Test
    void detectsExceptionLogsWithoutStackTrace() {
        CheckVerifier.newVerifier()
                .onFile(TEST_FILE_WITH_ISSUES)
                .withCheck(new LogExceptionWithStackTraceCheck())
                .verifyIssues();
    }

    @Test
    void checkClassCanBeInstantiated() {
        LogExceptionWithStackTraceCheck check = new LogExceptionWithStackTraceCheck();
        assertThat(check).isNotNull();
    }

    @Test
    void ruleKeyAnnotation() {
        LogExceptionWithStackTraceCheck check = new LogExceptionWithStackTraceCheck();
        org.sonar.check.Rule ruleAnnotation = check.getClass().getAnnotation(org.sonar.check.Rule.class);
        assertThat(ruleAnnotation).isNotNull();
        assertThat(ruleAnnotation.key()).isEqualTo("LogExceptionWithStackTrace");
    }
}
