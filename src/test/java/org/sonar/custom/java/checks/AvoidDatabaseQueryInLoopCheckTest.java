package org.sonar.custom.java.checks;

import org.sonar.custom.java.checks.performance.AvoidDatabaseQueryInLoopCheck;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidDatabaseQueryInLoopCheckTest {

    private static final String TEST_FILE_WITH_ISSUES = "src/test/resources/checks/AvoidDatabaseQueryInLoopCheck.java";

    @Test
    void detectsJdbcQueriesInsideLoops() {
        CheckVerifier.newVerifier()
                .onFile(TEST_FILE_WITH_ISSUES)
                .withCheck(new AvoidDatabaseQueryInLoopCheck())
                .verifyIssues();
    }

    @Test
    void checkClassCanBeInstantiated() {
        AvoidDatabaseQueryInLoopCheck check = new AvoidDatabaseQueryInLoopCheck();
        assertThat(check).isNotNull();
    }

    @Test
    void ruleKeyAnnotation() {
        AvoidDatabaseQueryInLoopCheck check = new AvoidDatabaseQueryInLoopCheck();
        org.sonar.check.Rule ruleAnnotation = check.getClass().getAnnotation(org.sonar.check.Rule.class);
        assertThat(ruleAnnotation).isNotNull();
        assertThat(ruleAnnotation.key()).isEqualTo("AvoidDatabaseQueryInLoop");
    }
}

