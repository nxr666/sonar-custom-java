package org.sonar.custom.java.checks.reliability;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "AvoidTransactionalOnControllerMethod")
public class AvoidTransactionalOnControllerMethodCheck extends IssuableSubscriptionVisitor {

    private static final String MESSAGE =
            "Do not put @Transactional on Controller methods. Move transaction boundary to Service-layer methods.";

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
        ClassTree classTree = (ClassTree) tree;
        if (!isController(classTree.modifiers())) {
            return;
        }
        for (Tree member : classTree.members()) {
            if (!member.is(Tree.Kind.METHOD)) {
                continue;
            }
            MethodTree methodTree = (MethodTree) member;
            if (hasTransactional(methodTree.modifiers())) {
                reportIssue(methodTree.simpleName(), MESSAGE);
            }
        }
    }

    private static boolean isController(ModifiersTree modifiers) {
        for (AnnotationTree annotation : modifiers.annotations()) {
            String name = annotation.annotationType().toString().toLowerCase();
            if ("controller".equals(name)
                    || name.endsWith(".controller")
                    || "restcontroller".equals(name)
                    || name.endsWith(".restcontroller")) {
                return true;
            }
        }
        return false;
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
