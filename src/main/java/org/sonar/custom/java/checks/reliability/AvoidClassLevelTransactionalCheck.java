package org.sonar.custom.java.checks.reliability;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "AvoidClassLevelTransactional")
public class AvoidClassLevelTransactionalCheck extends IssuableSubscriptionVisitor {

    private static final String MESSAGE =
            "Do not put @Transactional on class declarations. Declare transactions on methods explicitly.";

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
        ClassTree classTree = (ClassTree) tree;
        if (hasTransactional(classTree.modifiers())) {
            reportIssue(classTree.simpleName(), MESSAGE);
        }
    }

    private static boolean hasTransactional(ModifiersTree modifiers) {
        for (AnnotationTree annotation : modifiers.annotations()) {
            String name = annotation.annotationType().toString().toLowerCase();
            if ("transactional".equals(name) || name.endsWith(".transactional")) {
                return true;
            }
        }
        return false;
    }
}
