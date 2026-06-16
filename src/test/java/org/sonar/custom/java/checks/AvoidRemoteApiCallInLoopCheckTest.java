package org.sonar.custom.java.checks;

import org.sonar.custom.java.checks.performance.AvoidRemoteApiCallInLoopCheck;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidRemoteApiCallInLoopCheckTest {

    private static final String TEST_FILE_WITH_ISSUES = "src/test/resources/checks/AvoidRemoteApiCallInLoopCheck.java";

    @Test
    void detectsRemoteCallsInsideLoops() {
        CheckVerifier.newVerifier()
                .onFile(TEST_FILE_WITH_ISSUES)
                .withCheck(new AvoidRemoteApiCallInLoopCheck())
                .verifyIssues();
    }

    @Test
    void checkClassCanBeInstantiated() {
        AvoidRemoteApiCallInLoopCheck check = new AvoidRemoteApiCallInLoopCheck();
        assertThat(check).isNotNull();
    }

    @Test
    void ruleKeyAnnotation() {
        AvoidRemoteApiCallInLoopCheck check = new AvoidRemoteApiCallInLoopCheck();
        org.sonar.check.Rule ruleAnnotation = check.getClass().getAnnotation(org.sonar.check.Rule.class);
        assertThat(ruleAnnotation).isNotNull();
        assertThat(ruleAnnotation.key()).isEqualTo("AvoidRemoteApiCallInLoop");
    }
}

