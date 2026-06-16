package org.sonar.custom.java;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Defines the rules repository for custom rules.
 */
public class CustomJavaRulesDefinition implements RulesDefinition {

    public static final String REPOSITORY_KEY = "custom-java";
    public static final String REPOSITORY_NAME = "Custom Java Rules";
    private static final String RESOURCE_FOLDER = "org/sonar/l10n/java/rules/custom";

    @Override
    public void define(Context context) {
        NewRepository repository = context.createRepository(REPOSITORY_KEY, "java")
                .setName(REPOSITORY_NAME);

        RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER);

        ruleMetadataLoader.addRulesByAnnotatedClass(repository, getCheckClasses());

        repository.done();
    }

    /**
     * Returns the list of all check classes that contain rules.
     */
    public static List<Class<?>> getCheckClasses() {
        List<Class<?>> checks = new ArrayList<>();
        checks.add(org.sonar.custom.java.checks.performance.AvoidDatabaseQueryInLoopCheck.class);
        checks.add(org.sonar.custom.java.checks.performance.AvoidRemoteApiCallInLoopCheck.class);
        checks.add(org.sonar.custom.java.checks.reliability.LogExceptionWithStackTraceCheck.class);
        checks.add(org.sonar.custom.java.checks.reliability.AvoidTransactionalSelfInvocationCheck.class);
        checks.add(org.sonar.custom.java.checks.reliability.AvoidInvalidTransactionalTargetCheck.class);
        checks.add(org.sonar.custom.java.checks.reliability.AvoidWriteInReadOnlyTransactionCheck.class);
        checks.add(org.sonar.custom.java.checks.reliability.AvoidCrossThreadWriteInTransactionalMethodCheck.class);
        checks.add(org.sonar.custom.java.checks.reliability.AvoidSwallowingExceptionInTransactionalMethodCheck.class);
        checks.add(org.sonar.custom.java.checks.reliability.RequireRollbackForExceptionInTransactionalCheck.class);
        checks.add(org.sonar.custom.java.checks.reliability.AvoidClassLevelTransactionalCheck.class);
        checks.add(org.sonar.custom.java.checks.reliability.AvoidTransactionalOnControllerMethodCheck.class);
        return Collections.unmodifiableList(checks);
    }
}
