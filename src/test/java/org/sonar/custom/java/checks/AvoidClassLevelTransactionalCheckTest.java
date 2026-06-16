package org.sonar.custom.java.checks;

import org.sonar.custom.java.checks.reliability.AvoidClassLevelTransactionalCheck;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidClassLevelTransactionalCheckTest {

    private static final String TEST_FILE = "src/test/resources/checks/AvoidClassLevelTransactionalCheck.java";

    @Test
    void detectsClassLevelTransactional() {
        CheckVerifier.newVerifier()
                .onFile(TEST_FILE)
                .withCheck(new AvoidClassLevelTransactionalCheck())
                .verifyIssues();
    }

    @Test
    void checkClassCanBeInstantiated() {
        AvoidClassLevelTransactionalCheck check = new AvoidClassLevelTransactionalCheck();
        assertThat(check).isNotNull();
    }

    @Test
    void ruleKeyAnnotation() {
        AvoidClassLevelTransactionalCheck check = new AvoidClassLevelTransactionalCheck();
        org.sonar.check.Rule ruleAnnotation = check.getClass().getAnnotation(org.sonar.check.Rule.class);
        assertThat(ruleAnnotation).isNotNull();
        assertThat(ruleAnnotation.key()).isEqualTo("AvoidClassLevelTransactional");
    }
}
