package org.sonar.custom.java.checks.reliability;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "AvoidWriteInReadOnlyTransaction")
public class AvoidWriteInReadOnlyTransactionCheck extends IssuableSubscriptionVisitor {

    private static final String MESSAGE =
            "Do not perform write operations in @Transactional(readOnly = true) methods.";

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.METHOD);
    }

    @Override
    public void visitNode(Tree tree) {
        MethodTree methodTree = (MethodTree) tree;
        if (!isReadOnlyTransactionalByNearbySource(methodTree)) {
            return;
        }
        if (methodTree.block() == null) {
            return;
        }
        methodTree.block().accept(new org.sonar.plugins.java.api.tree.BaseTreeVisitor() {
            @Override
            public void visitMethodInvocation(MethodInvocationTree invocation) {
                if (looksLikeWrite(methodName(invocation)) && isLikelyPersistenceWriteTarget(invocation)) {
                    reportIssue(invocation, MESSAGE);
                }
                super.visitMethodInvocation(invocation);
            }
        });
    }

    private boolean isReadOnlyTransactionalByNearbySource(MethodTree methodTree) {
        if (methodTree.firstToken() == null || context == null) {
            return false;
        }
        List<String> fileLines = context.getFileLines();
        int methodLine = methodTree.firstToken().line();
        int from = Math.max(1, methodLine - 4);
        int to = Math.min(fileLines.size(), methodLine + 1);
        for (int i = from; i <= to; i++) {
            String line = fileLines.get(i - 1).toLowerCase().replace(" ", "");
            if (line.contains("@transactional") && line.contains("readonly=true")) {
                return true;
            }
        }
        return false;
    }

    private static String methodName(MethodInvocationTree invocation) {
        if (invocation.methodSelect().is(Tree.Kind.IDENTIFIER)) {
            return ((IdentifierTree) invocation.methodSelect()).name();
        }
        if (invocation.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
            return ((MemberSelectExpressionTree) invocation.methodSelect()).identifier().name();
        }
        return "";
    }

    private static boolean looksLikeWrite(String methodName) {
        String lower = methodName.toLowerCase();
        return lower.startsWith("insert")
                || lower.startsWith("update")
                || lower.startsWith("delete")
                || lower.startsWith("remove")
                || lower.startsWith("save")
                || lower.startsWith("create")
                || lower.startsWith("batch")
                || "executeupdate".equals(lower);
    }

    private static boolean isLikelyPersistenceWriteTarget(MethodInvocationTree invocation) {
        if (!invocation.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
            return false;
        }
        MemberSelectExpressionTree select = (MemberSelectExpressionTree) invocation.methodSelect();
        String qualifier = select.expression().toString().toLowerCase();
        return qualifier.endsWith("mapper")
                || qualifier.endsWith("dao")
                || qualifier.endsWith("repository")
                || qualifier.endsWith("sqlsession")
                || qualifier.endsWith("entitymanager")
                || qualifier.endsWith("jdbctemplate");
    }
}
