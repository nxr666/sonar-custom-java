package org.sonar.custom.java.checks.reliability;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

import java.util.Collections;
import java.util.List;

@Rule(key = "LogExceptionWithStackTrace")
public class LogExceptionWithStackTraceCheck extends IssuableSubscriptionVisitor {

    private static final String MESSAGE =
            "When catching Exception, log the stack trace by passing the exception object (for example: log.error(\"...\", e)).";

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.CATCH);
    }

    @Override
    public void visitNode(Tree tree) {
        CatchTree catchTree = (CatchTree) tree;
        Type exceptionType = catchTree.parameter().symbol().type();
        if (exceptionType == null || !exceptionType.is("java.lang.Exception")) {
            return;
        }

        Symbol exceptionSymbol = catchTree.parameter().symbol();
        catchTree.block().accept(new CatchLoggingVisitor(exceptionSymbol));
    }

    private final class CatchLoggingVisitor extends BaseTreeVisitor {
        private final Symbol exceptionSymbol;

        private CatchLoggingVisitor(Symbol exceptionSymbol) {
            this.exceptionSymbol = exceptionSymbol;
        }

        @Override
        public void visitMethodInvocation(MethodInvocationTree tree) {
            if (isLoggerCall(tree) && !hasThrowableArgument(tree.arguments(), exceptionSymbol)) {
                reportIssue(tree, MESSAGE);
            }
            super.visitMethodInvocation(tree);
        }
    }

    private static boolean isLoggerCall(MethodInvocationTree mit) {
        Symbol symbol = mit.symbol();
        if (!(symbol instanceof Symbol.MethodSymbol)) {
            return false;
        }

        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
        String methodName = methodSymbol.name();
        if (!isLoggingLevelMethod(methodName)) {
            return false;
        }

        Type ownerType = methodSymbol.owner() != null ? methodSymbol.owner().type() : null;
        return ownerType != null
                && (ownerType.isSubtypeOf("org.slf4j.Logger")
                || ownerType.isSubtypeOf("org.apache.logging.log4j.Logger")
                || ownerType.isSubtypeOf("java.util.logging.Logger"));
    }

    private static boolean isLoggingLevelMethod(String methodName) {
        return "error".equals(methodName)
                || "warn".equals(methodName)
                || "info".equals(methodName)
                || "debug".equals(methodName)
                || "trace".equals(methodName)
                || "log".equals(methodName)
                || "severe".equals(methodName)
                || "warning".equals(methodName);
    }

    private static boolean hasThrowableArgument(List<ExpressionTree> arguments, Symbol exceptionSymbol) {
        for (ExpressionTree argument : arguments) {
            if (isDirectExceptionReference(argument, exceptionSymbol)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDirectExceptionReference(ExpressionTree tree, Symbol exceptionSymbol) {
        ExpressionTree current = tree;
        while (current.is(Tree.Kind.PARENTHESIZED_EXPRESSION) || current.is(Tree.Kind.TYPE_CAST)) {
            if (current.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
                current = ((ParenthesizedTree) current).expression();
            } else {
                current = ((TypeCastTree) current).expression();
            }
        }

        if (!current.is(Tree.Kind.IDENTIFIER)) {
            return false;
        }
        IdentifierTree identifier = (IdentifierTree) current;
        return identifier.symbol().equals(exceptionSymbol);
    }
}
