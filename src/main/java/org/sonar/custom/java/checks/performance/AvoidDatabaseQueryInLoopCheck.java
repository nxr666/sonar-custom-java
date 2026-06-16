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

@Rule(key = "AvoidDatabaseQueryInLoop")
public class AvoidDatabaseQueryInLoopCheck extends IssuableSubscriptionVisitor {

    private static final String MESSAGE =
            "Avoid database queries inside loops (N+1). Fetch required data in bulk before the loop or batch queries.";

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

        body.accept(new LoopBodyDbInvocationVisitor());
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

    private final class LoopBodyDbInvocationVisitor extends org.sonar.plugins.java.api.tree.BaseTreeVisitor {
        @Override
        public void visitMethodInvocation(MethodInvocationTree tree) {
            // This visitor only runs on a loop body, so every invocation encountered is inside a loop.
            if (isDatabaseQueryInvocation(tree) && alreadyReported.add(tree)) {
                reportIssue(tree, MESSAGE);
            }
            super.visitMethodInvocation(tree);
        }
    }

    private static boolean isDatabaseQueryInvocation(MethodInvocationTree mit) {
        Symbol raw = mit.symbol();
        if (!(raw instanceof Symbol.MethodSymbol)) {
            return false;
        }
        Symbol.MethodSymbol symbol = (Symbol.MethodSymbol) raw;
        String methodName = symbol.name();

        Type ownerType = symbol.owner() != null ? symbol.owner().type() : null;
        if (ownerType == null) {
            return false;
        }
        if (!looksLikeQueryMethod(methodName)) {
            return false;
        }

        String ownerName = safeName(ownerType);

        // Type + method-name detection.
        // Only query-like methods on DB access types are considered N+1 candidates.
        // JDBC
        if (isSubtype(ownerType, "java.sql.Statement")
                || isSubtype(ownerType, "java.sql.PreparedStatement")
                || isSubtype(ownerType, "java.sql.Connection")) {
            return true;
        }

        // JPA / Hibernate (jakarta + javax)
        if (isSubtype(ownerType, "jakarta.persistence.EntityManager")
                || isSubtype(ownerType, "javax.persistence.EntityManager")
                || isSubtype(ownerType, "org.hibernate.Session")) {
            return true;
        }

        // MyBatis
        if (isSubtype(ownerType, "org.apache.ibatis.session.SqlSession")
                || isSubtype(ownerType, "org.mybatis.spring.SqlSessionTemplate")) {
            return true;
        }

        // MyBatis-Plus
        if (isSubtype(ownerType, "com.baomidou.mybatisplus.core.mapper.BaseMapper")) {
            return true;
        }

        // Spring JDBC template
        if (isSubtype(ownerType, "org.springframework.jdbc.core.JdbcTemplate")
                || isSubtype(ownerType, "org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate")) {
            return true;
        }

        // Spring Data repositories (best-effort; depends on semantic classpath)
        if (isSubtype(ownerType, "org.springframework.data.repository.Repository")
                || isSubtype(ownerType, "org.springframework.data.repository.CrudRepository")) {
            return true;
        }

        // Fallback for common project conventions.
        if (looksLikeMapperOrDao(ownerName)) {
            return true;
        }

        return false;
    }

    private static boolean looksLikeMapperOrDao(String ownerName) {
        String name = ownerName != null ? ownerName : "";
        String lowerName = name.toLowerCase();
        return lowerName.endsWith("mapper") || lowerName.endsWith("dao");
    }

    private static boolean looksLikeQueryMethod(String methodName) {
        if (methodName == null || methodName.trim().isEmpty()) {
            return false;
        }
        String lowerName = methodName.toLowerCase();
        return lowerName.startsWith("select")
                || lowerName.startsWith("find")
                || lowerName.startsWith("get")
                || lowerName.startsWith("query")
                || lowerName.startsWith("list")
                || lowerName.startsWith("page")
                || lowerName.startsWith("count")
                || lowerName.startsWith("exists")
                || lowerName.equals("executequery")
                || lowerName.equals("queryforobject")
                || lowerName.equals("queryforlist")
                || lowerName.equals("queryformap")
                || lowerName.equals("queryforrowset");
    }

    private static boolean isSubtype(Type type, String fqn) {
        try {
            return type != null && type.isSubtypeOf(fqn);
        } catch (Exception ignored) {
            // Defensive: semantic information might be incomplete in some contexts.
            return false;
        }
    }

    private static String safeName(Type type) {
        try {
            return type.name();
        } catch (Exception ignored) {
            return null;
        }
    }
}

