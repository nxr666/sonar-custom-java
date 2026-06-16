package org.sonar.custom.java.checks;

import org.sonar.custom.java.checks.reliability.AvoidSwallowingExceptionInTransactionalMethodCheck;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidSwallowingExceptionInTransactionalMethodCheckTest {

    private static final String TEST_FILE = "src/test/resources/checks/AvoidSwallowingExceptionInTransactionalMethodCheck.java";

    @Test
    void detectsSwallowingExceptionAndReturningSuccess() {
        CheckVerifier.newVerifier()
                .onFile(TEST_FILE)
                .withCheck(new AvoidSwallowingExceptionInTransactionalMethodCheck())
                .verifyIssues();
    }

    @Test
    void checkClassCanBeInstantiated() {
        AvoidSwallowingExceptionInTransactionalMethodCheck check = new AvoidSwallowingExceptionInTransactionalMethodCheck();
        assertThat(check).isNotNull();
    }

    @Test
    void ruleKeyAnnotation() {
        AvoidSwallowingExceptionInTransactionalMethodCheck check = new AvoidSwallowingExceptionInTransactionalMethodCheck();
        org.sonar.check.Rule ruleAnnotation = check.getClass().getAnnotation(org.sonar.check.Rule.class);
        assertThat(ruleAnnotation).isNotNull();
        assertThat(ruleAnnotation.key()).isEqualTo("AvoidSwallowingExceptionInTransactionalMethod");
    }
}
