package org.sonar.custom.java.checks;

import org.sonar.custom.java.checks.reliability.AvoidCrossThreadWriteInTransactionalMethodCheck;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidCrossThreadWriteInTransactionalMethodCheckTest {

    private static final String TEST_FILE = "src/test/resources/checks/AvoidCrossThreadWriteInTransactionalMethodCheck.java";

    @Test
    void detectsCrossThreadWritesInTransactionalMethod() {
        CheckVerifier.newVerifier()
                .onFile(TEST_FILE)
                .withCheck(new AvoidCrossThreadWriteInTransactionalMethodCheck())
                .verifyIssues();
    }

    @Test
    void checkClassCanBeInstantiated() {
        AvoidCrossThreadWriteInTransactionalMethodCheck check = new AvoidCrossThreadWriteInTransactionalMethodCheck();
        assertThat(check).isNotNull();
    }

    @Test
    void ruleKeyAnnotation() {
        AvoidCrossThreadWriteInTransactionalMethodCheck check = new AvoidCrossThreadWriteInTransactionalMethodCheck();
        org.sonar.check.Rule ruleAnnotation = check.getClass().getAnnotation(org.sonar.check.Rule.class);
        assertThat(ruleAnnotation).isNotNull();
        assertThat(ruleAnnotation.key()).isEqualTo("AvoidCrossThreadWriteInTransactionalMethod");
    }
}
