package org.sonar.custom.java;

import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonarsource.api.sonarlint.SonarLintSide;

import java.util.Collections;
import java.util.List;

/**
 * Registers the custom checks with the SonarJava analyzer.
 */
@SonarLintSide
public class CustomJavaFileCheckRegistrar implements CheckRegistrar {

    @Override
    public void register(RegistrarContext registrarContext) {
        registrarContext.registerClassesForRepository(
                CustomJavaRulesDefinition.REPOSITORY_KEY,
                checkClasses(),
                testCheckClasses()
        );
    }

    /**
     * Returns the list of main source code checks.
     */
    @SuppressWarnings("unchecked")
    public static List<Class<? extends JavaCheck>> checkClasses() {
        return (List<Class<? extends JavaCheck>>) (List<?>) CustomJavaRulesDefinition.getCheckClasses();
    }

    /**
     * Returns the list of test source code checks.
     * Currently empty as we don't have test-specific rules.
     */
    public static List<Class<? extends JavaCheck>> testCheckClasses() {
        return Collections.emptyList();
    }
}
