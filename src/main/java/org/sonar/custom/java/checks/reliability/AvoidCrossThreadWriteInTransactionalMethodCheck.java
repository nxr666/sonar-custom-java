package org.sonar.custom.java.checks.reliability;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "AvoidCrossThreadWriteInTransactionalMethod")
public class AvoidCrossThreadWriteInTransactionalMethodCheck extends IssuableSubscriptionVisitor {

    private static final String MESSAGE =
            "Write operations in new threads or async tasks are outside current @Transactional context and may not participate in the transaction.";

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.METHOD);
    }

    @Override
    public void visitNode(Tree tree) {
        MethodTree methodTree = (MethodTree) tree;
        if (methodTree.block() == null || !hasTransactional(methodTree)) {
            return;
        }

        AsyncWriteVisitor visitor = new AsyncWriteVisitor();
        methodTree.block().accept(visitor);
        if (visitor.hasAsyncWrite) {
            reportIssue(methodTree.simpleName(), MESSAGE);
        }
    }

    private static boolean hasTransactional(MethodTree methodTree) {
        for (AnnotationTree annotation : methodTree.modifiers().annotations()) {
            String annotationText = annotation.annotationType().toString().toLowerCase();
            if (annotationText.endsWith("transactional") || annotationText.contains(".transactional")) {
                return true;
            }
        }
        return false;
    }

    private static final class AsyncWriteVisitor extends BaseTreeVisitor {
        private boolean hasAsyncWrite;

        @Override
        public void visitMethodInvocation(MethodInvocationTree tree) {
            if (hasAsyncWrite) {
                return;
            }
            if (isAsyncBoundary(tree) && hasWriteOperationInArguments(tree)) {
                hasAsyncWrite = true;
                return;
            }
            super.visitMethodInvocation(tree);
        }

        @Override
        public void visitNewClass(NewClassTree tree) {
            if (hasAsyncWrite) {
                return;
            }
            if ("Thread".equalsIgnoreCase(tree.identifier().toString()) && hasWriteOperationInArguments(tree.arguments())) {
                hasAsyncWrite = true;
                return;
            }
            super.visitNewClass(tree);
        }
    }

    private static boolean isAsyncBoundary(MethodInvocationTree tree) {
        if (!tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
            return false;
        }
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) tree.methodSelect();
        String methodName = memberSelect.identifier().name();
        String owner = memberSelect.expression().toString().toLowerCase();
        return ("submit".equals(methodName) || "execute".equals(methodName))
                || ("runAsync".equals(methodName) || "supplyAsync".equals(methodName))
                || ("start".equals(methodName) && owner.contains("thread"));
    }

    private static boolean hasWriteOperationInArguments(MethodInvocationTree tree) {
        return hasWriteOperationInArguments(tree.arguments());
    }

    private static boolean hasWriteOperationInArguments(List<? extends Tree> arguments) {
        WriteCallVisitor visitor = new WriteCallVisitor();
        for (Tree argument : arguments) {
            argument.accept(visitor);
            if (visitor.hasWriteCall) {
                return true;
            }
        }
        return false;
    }

    private static final class WriteCallVisitor extends BaseTreeVisitor {
        private boolean hasWriteCall;

        @Override
        public void visitMethodInvocation(MethodInvocationTree tree) {
            if (hasWriteCall) {
                return;
            }
            if (isPersistenceWriteCall(tree)) {
                hasWriteCall = true;
                return;
            }
            super.visitMethodInvocation(tree);
        }
    }

    private static boolean isPersistenceWriteCall(MethodInvocationTree tree) {
        String methodName = extractMethodName(tree).toLowerCase();
        if (!(methodName.startsWith("insert")
                || methodName.startsWith("update")
                || methodName.startsWith("delete")
                || methodName.startsWith("remove")
                || methodName.startsWith("save")
                || methodName.startsWith("create")
                || methodName.startsWith("batch")
                || methodName.startsWith("executeUpdate".toLowerCase()))) {
            return false;
        }
        if (!tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
            return false;
        }
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) tree.methodSelect();
        String owner = memberSelect.expression().toString().toLowerCase();
        return owner.endsWith("mapper")
                || owner.endsWith("dao")
                || owner.endsWith("repository")
                || owner.endsWith("sqlsession")
                || owner.endsWith("entitymanager")
                || owner.endsWith("jdbctemplate");
    }

    private static String extractMethodName(MethodInvocationTree tree) {
        if (tree.methodSelect().is(Tree.Kind.IDENTIFIER)) {
            return ((IdentifierTree) tree.methodSelect()).name();
        }
        if (tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
            return ((MemberSelectExpressionTree) tree.methodSelect()).identifier().name();
        }
        return tree.methodSelect().toString();
    }
}
