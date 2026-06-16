package org.sonar.custom.java.checks;

import org.sonar.custom.java.checks.reliability.AvoidInvalidTransactionalTargetCheck;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidInvalidTransactionalTargetCheckTest {

    private static final String TEST_FILE = "src/test/resources/checks/AvoidInvalidTransactionalTargetCheck.java";

    @Test
    void detectsInvalidTransactionalTargets() {
        CheckVerifier.newVerifier()
                .onFile(TEST_FILE)
                .withCheck(new AvoidInvalidTransactionalTargetCheck())
                .verifyIssues();
    }

    @Test
    void checkClassCanBeInstantiated() {
        AvoidInvalidTransactionalTargetCheck check = new AvoidInvalidTransactionalTargetCheck();
        assertThat(check).isNotNull();
    }

    @Test
    void ruleKeyAnnotation() {
        AvoidInvalidTransactionalTargetCheck check = new AvoidInvalidTransactionalTargetCheck();
        org.sonar.check.Rule ruleAnnotation = check.getClass().getAnnotation(org.sonar.check.Rule.class);
        assertThat(ruleAnnotation).isNotNull();
        assertThat(ruleAnnotation.key()).isEqualTo("AvoidInvalidTransactionalTarget");
    }
}
