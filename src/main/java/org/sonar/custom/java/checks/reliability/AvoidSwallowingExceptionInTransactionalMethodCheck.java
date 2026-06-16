package org.sonar.custom.java.checks.reliability;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "AvoidSwallowingExceptionInTransactionalMethod")
public class AvoidSwallowingExceptionInTransactionalMethodCheck extends IssuableSubscriptionVisitor {
    private static final String MESSAGE =
            "Do not swallow exceptions in @Transactional methods without rethrowing or setting rollback-only.";

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.METHOD);
    }

    @Override
    public void visitNode(Tree tree) {
        MethodTree methodTree = (MethodTree) tree;
        if (methodTree.block() == null || !isTransactionalMethod(methodTree)) {
            return;
        }

        CatchAnalysisVisitor visitor = new CatchAnalysisVisitor();
        methodTree.block().accept(visitor);
        if (visitor.hasInvalidCatch) {
            reportIssue(methodTree, MESSAGE);
        }
    }

    private static boolean isTransactionalMethod(MethodTree methodTree) {
        for (AnnotationTree annotation : methodTree.modifiers().annotations()) {
            String annotationText = annotation.annotationType().toString().toLowerCase();
            if (annotationText.endsWith("transactional")
                    || annotationText.contains(".transactional")) {
                return true;
            }
        }
        return false;
    }

    private static final class CatchAnalysisVisitor extends BaseTreeVisitor {
        private boolean hasInvalidCatch;

        @Override
        public void visitCatch(CatchTree tree) {
            if (hasInvalidCatch || !isExceptionOrThrowableCatch(tree)) {
                super.visitCatch(tree);
                return;
            }

            CatchBodyVisitor catchBodyVisitor = new CatchBodyVisitor();
            tree.block().accept(catchBodyVisitor);
            if (!catchBodyVisitor.hasThrow && !catchBodyVisitor.hasSetRollbackOnly) {
                hasInvalidCatch = true;
            }
            super.visitCatch(tree);
        }

        private static boolean isExceptionOrThrowableCatch(CatchTree catchTree) {
            Symbol symbol = catchTree.parameter().symbol();
            Type type = symbol != null ? symbol.type() : null;
            if (type != null && (type.is("java.lang.Exception") || type.is("java.lang.Throwable"))) {
                return true;
            }
            String typeText = catchTree.parameter().type().toString().toLowerCase();
            return "exception".equals(typeText)
                    || "throwable".equals(typeText)
                    || typeText.endsWith(".exception")
                    || typeText.endsWith(".throwable");
        }
    }

    private static final class CatchBodyVisitor extends BaseTreeVisitor {
        private boolean hasThrow;
        private boolean hasSetRollbackOnly;

        @Override
        public void visitThrowStatement(ThrowStatementTree tree) {
            hasThrow = true;
            super.visitThrowStatement(tree);
        }

        @Override
        public void visitMethodInvocation(MethodInvocationTree tree) {
            Symbol symbol = tree.symbol();
            Symbol.MethodSymbol methodSymbol = symbol instanceof Symbol.MethodSymbol
                    ? (Symbol.MethodSymbol) symbol
                    : null;
            if (methodSymbol != null && "setRollbackOnly".equals(methodSymbol.name())) {
                hasSetRollbackOnly = true;
            }
            super.visitMethodInvocation(tree);
        }
    }
}
