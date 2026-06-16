package org.sonar.custom.java.checks.reliability;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(key = "AvoidTransactionalSelfInvocation")
public class AvoidTransactionalSelfInvocationCheck extends IssuableSubscriptionVisitor {

    private static final String MESSAGE =
            "Self-invocation bypasses Spring proxy; @Transactional on the called method may not take effect.";

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
        ClassTree classTree = (ClassTree) tree;
        Set<String> transactionalMethodNames = new HashSet<>();

        for (Tree member : classTree.members()) {
            if (!member.is(Tree.Kind.METHOD)) {
                continue;
            }
            MethodTree methodTree = (MethodTree) member;
            if (hasTransactionalAnnotation(methodTree.modifiers())) {
                transactionalMethodNames.add(methodTree.simpleName().name());
            }
        }

        if (transactionalMethodNames.isEmpty()) {
            return;
        }

        for (Tree member : classTree.members()) {
            if (!member.is(Tree.Kind.METHOD)) {
                continue;
            }
            MethodTree methodTree = (MethodTree) member;
            if (methodTree.block() != null) {
                methodTree.block().accept(new SelfInvocationVisitor(transactionalMethodNames));
            }
        }
    }

    private static boolean hasTransactionalAnnotation(ModifiersTree modifiersTree) {
        return modifiersTree.annotations().stream().anyMatch(annotationTree -> {
            String annotationName = annotationTree.annotationType().toString();
            return "Transactional".equals(annotationName) || annotationName.endsWith(".Transactional");
        });
    }

    private final class SelfInvocationVisitor extends BaseTreeVisitor {
        private final Set<String> transactionalMethodNames;

        private SelfInvocationVisitor(Set<String> transactionalMethodNames) {
            this.transactionalMethodNames = transactionalMethodNames;
        }

        @Override
        public void visitMethodInvocation(MethodInvocationTree tree) {
            if (isSelfInvocation(tree, transactionalMethodNames)) {
                reportIssue(tree, MESSAGE);
            }
            super.visitMethodInvocation(tree);
        }
    }

    private static boolean isSelfInvocation(MethodInvocationTree tree, Set<String> transactionalMethodNames) {
        if (tree.methodSelect().is(Tree.Kind.IDENTIFIER)) {
            return transactionalMethodNames.contains(((IdentifierTree) tree.methodSelect()).name());
        }
        if (tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
            MemberSelectExpressionTree select = (MemberSelectExpressionTree) tree.methodSelect();
            return transactionalMethodNames.contains(select.identifier().name())
                    && "this".equals(select.expression().toString());
        }
        return false;
    }
}
