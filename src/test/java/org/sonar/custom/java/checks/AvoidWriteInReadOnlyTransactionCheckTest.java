package org.sonar.custom.java.checks;

import org.sonar.custom.java.checks.reliability.AvoidWriteInReadOnlyTransactionCheck;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidWriteInReadOnlyTransactionCheckTest {

    private static final String TEST_FILE = "src/test/resources/checks/AvoidWriteInReadOnlyTransactionCheck.java";

    @Test
    void detectsWriteInvocationInReadOnlyTransaction() {
        CheckVerifier.newVerifier()
                .onFile(TEST_FILE)
                .withCheck(new AvoidWriteInReadOnlyTransactionCheck())
                .verifyIssues();
    }

    @Test
    void checkClassCanBeInstantiated() {
        AvoidWriteInReadOnlyTransactionCheck check = new AvoidWriteInReadOnlyTransactionCheck();
        assertThat(check).isNotNull();
    }

    @Test
    void ruleKeyAnnotation() {
        AvoidWriteInReadOnlyTransactionCheck check = new AvoidWriteInReadOnlyTransactionCheck();
        org.sonar.check.Rule ruleAnnotation = check.getClass().getAnnotation(org.sonar.check.Rule.class);
        assertThat(ruleAnnotation).isNotNull();
        assertThat(ruleAnnotation.key()).isEqualTo("AvoidWriteInReadOnlyTransaction");
    }
}
