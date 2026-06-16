package org.sonar.custom.java.checks.performance;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(key = "AvoidRemoteApiCallInLoop")
public class AvoidRemoteApiCallInLoopCheck extends IssuableSubscriptionVisitor {

    private static final String MESSAGE =
            "Avoid remote API calls inside loops. Batch requests, cache data, or move calls outside loop to prevent N+1 network calls.";

    private final Set<Tree> alreadyReported = new HashSet<>();

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(
                Tree.Kind.FOR_STATEMENT,
                Tree.Kind.FOR_EACH_STATEMENT,
                Tree.Kind.WHILE_STATEMENT,
                Tree.Kind.DO_STATEMENT
        );
    }

    @Override
    public void visitNode(Tree tree) {
        StatementTree body = loopBody(tree);
        if (body == null) {
            return;
        }
        body.accept(new LoopBodyRemoteCallVisitor());
    }

    private StatementTree loopBody(Tree tree) {
        if (tree.is(Tree.Kind.FOR_STATEMENT)) {
            return ((ForStatementTree) tree).statement();
        }
        if (tree.is(Tree.Kind.FOR_EACH_STATEMENT)) {
            return ((ForEachStatement) tree).statement();
        }
        if (tree.is(Tree.Kind.WHILE_STATEMENT)) {
            return ((WhileStatementTree) tree).statement();
        }
        if (tree.is(Tree.Kind.DO_STATEMENT)) {
            return ((org.sonar.plugins.java.api.tree.DoWhileStatementTree) tree).statement();
        }
        return null;
    }

    private final class LoopBodyRemoteCallVisitor extends org.sonar.plugins.java.api.tree.BaseTreeVisitor {
        @Override
        public void visitMethodInvocation(MethodInvocationTree tree) {
            if (isRemoteApiInvocation(tree) && alreadyReported.add(tree)) {
                reportIssue(tree, MESSAGE);
            }
            super.visitMethodInvocation(tree);
        }
    }

    private static boolean isRemoteApiInvocation(MethodInvocationTree mit) {
        Symbol raw = mit.symbol();
        if (!(raw instanceof Symbol.MethodSymbol)) {
            return false;
        }
        Symbol.MethodSymbol symbol = (Symbol.MethodSymbol) raw;
        Type ownerType = symbol.owner() != null ? symbol.owner().type() : null;
        if (ownerType == null) {
            return false;
        }

        // Spring HTTP clients
        if (isSubtype(ownerType, "org.springframework.web.client.RestTemplate")
                || isSubtype(ownerType, "org.springframework.web.reactive.function.client.WebClient")) {
            return true;
        }

        // Feign
        if (isSubtype(ownerType, "feign.Client")
                || isSubtype(ownerType, "org.springframework.cloud.openfeign.FeignClientFactoryBean")) {
            return true;
        }

        // Common HTTP clients
        if (isSubtype(ownerType, "java.net.http.HttpClient")
                || isSubtype(ownerType, "okhttp3.OkHttpClient")
                || isSubtype(ownerType, "org.apache.http.client.HttpClient")
                || isSubtype(ownerType, "org.apache.http.impl.client.CloseableHttpClient")) {
            return true;
        }
        return false;
    }

    private static boolean isSubtype(Type type, String fqn) {
        try {
            return type != null && type.isSubtypeOf(fqn);
        } catch (Exception ignored) {
            return false;
        }
    }

}

