package org.sonar.custom.java.checks;

import org.sonar.custom.java.checks.reliability.AvoidTransactionalSelfInvocationCheck;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidTransactionalSelfInvocationCheckTest {

    private static final String TEST_FILE = "src/test/resources/checks/AvoidTransactionalSelfInvocationCheck.java";

    @Test
    void detectsTransactionalSelfInvocation() {
        CheckVerifier.newVerifier()
                .onFile(TEST_FILE)
                .withCheck(new AvoidTransactionalSelfInvocationCheck())
                .verifyIssues();
    }

    @Test
    void checkClassCanBeInstantiated() {
        AvoidTransactionalSelfInvocationCheck check = new AvoidTransactionalSelfInvocationCheck();
        assertThat(check).isNotNull();
    }

    @Test
    void ruleKeyAnnotation() {
        AvoidTransactionalSelfInvocationCheck check = new AvoidTransactionalSelfInvocationCheck();
        org.sonar.check.Rule ruleAnnotation = check.getClass().getAnnotation(org.sonar.check.Rule.class);
        assertThat(ruleAnnotation).isNotNull();
        assertThat(ruleAnnotation.key()).isEqualTo("AvoidTransactionalSelfInvocation");
    }
}
