package org.sonar.custom.java.checks;

import org.sonar.custom.java.checks.reliability.RequireRollbackForExceptionInTransactionalCheck;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class RequireRollbackForExceptionInTransactionalCheckTest {

    private static final String TEST_FILE = "src/test/resources/checks/RequireRollbackForExceptionInTransactionalCheck.java";

    @Test
    void detectsTransactionalWithoutRollbackForException() {
        CheckVerifier.newVerifier()
                .onFile(TEST_FILE)
                .withCheck(new RequireRollbackForExceptionInTransactionalCheck())
                .verifyIssues();
    }

    @Test
    void checkClassCanBeInstantiated() {
        RequireRollbackForExceptionInTransactionalCheck check = new RequireRollbackForExceptionInTransactionalCheck();
        assertThat(check).isNotNull();
    }

    @Test
    void ruleKeyAnnotation() {
        RequireRollbackForExceptionInTransactionalCheck check = new RequireRollbackForExceptionInTransactionalCheck();
        org.sonar.check.Rule ruleAnnotation = check.getClass().getAnnotation(org.sonar.check.Rule.class);
        assertThat(ruleAnnotation).isNotNull();
        assertThat(ruleAnnotation.key()).isEqualTo("RequireRollbackForExceptionInTransactional");
    }
}
