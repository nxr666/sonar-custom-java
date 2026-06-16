package org.sonar.custom.java.checks.reliability;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "RequireRollbackForExceptionInTransactional")
public class RequireRollbackForExceptionInTransactionalCheck extends IssuableSubscriptionVisitor {

    private static final String MESSAGE =
            "Add rollback rule to @Transactional (for example: rollbackFor = Exception.class) to avoid missing rollback on checked exceptions.";

    private static final int METHOD_LINES_BEFORE = 6;
    private static final int METHOD_LINES_AFTER = 2;

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
        if (context == null) {
            return;
        }
        ClassTree classTree = (ClassTree) tree;
        List<String> fileLines = context.getFileLines();
        if (fileLines.isEmpty()) {
            return;
        }
        int fileEnd = fileLines.size();
        int classFrom = classTree.firstToken() != null ? safeLine(classTree.firstToken().line()) : 1;
        int classTo = classTree.lastToken() != null ? safeLine(classTree.lastToken().line()) : fileEnd;
        if (classFrom < 1) {
            classFrom = 1;
        }
        if (classTo < classFrom) {
            classTo = fileEnd;
        }

        int headerTo = classTree.openBraceToken() != null
                ? classTree.openBraceToken().line()
                : Math.min(classTo, classFrom + 5);
        int classAnnotationStart = firstAnnotationLine(classTree.modifiers().annotations());
        int headerFrom = classAnnotationStart > 0
                ? Math.max(1, classAnnotationStart - 3)
                : Math.max(1, classFrom - 8);
        String classHeaderWindow = sourceWindow(fileLines, headerFrom, headerTo);
        if (hasTransactionalWithoutRequiredRollback(classHeaderWindow)) {
            reportIssue(classTree, MESSAGE);
        }

        int bodyStart = classTree.openBraceToken() != null ? classTree.openBraceToken().line() + 1 : classFrom;
        for (Tree member : classTree.members()) {
            if (!member.is(Tree.Kind.METHOD)) {
                continue;
            }
            MethodTree methodTree = (MethodTree) member;
            int methodLine = methodTree.simpleName() != null
                    ? methodTree.simpleName().firstToken().line()
                    : (methodTree.firstToken() != null ? methodTree.firstToken().line() : -1);
            if (methodLine < 1) {
                continue;
            }
            int annotationStart = firstAnnotationLine(methodTree.modifiers().annotations());
            int from = annotationStart > 0
                    ? Math.max(bodyStart, annotationStart - 2)
                    : Math.max(bodyStart, methodLine - METHOD_LINES_BEFORE);
            int to = Math.min(classTo, methodLine + METHOD_LINES_AFTER);
            String methodWindow = sourceWindow(fileLines, from, to);
            if (hasTransactionalWithoutRequiredRollback(methodWindow)) {
                reportIssue(methodTree.simpleName(), MESSAGE);
            }
        }
    }

    private static boolean hasTransactionalWithoutRequiredRollback(String window) {
        String compact = window.toLowerCase().replaceAll("\\s+", "");
        if (!compact.contains("@transactional")) {
            return false;
        }
        return !hasRollbackForExceptionOrThrowable(compact);
    }

    private static boolean hasRollbackForExceptionOrThrowable(String compactNoSpaces) {
        if (compactNoSpaces.contains("rollbackfor=exception.class")
                || compactNoSpaces.contains("rollbackfor=throwable.class")) {
            return true;
        }
        if (!compactNoSpaces.contains("rollbackforclassname=")) {
            return false;
        }
        return compactNoSpaces.contains("\"java.lang.exception\"")
                || compactNoSpaces.contains("\"java.lang.throwable\"");
    }

    private static int firstAnnotationLine(List<AnnotationTree> annotations) {
        for (AnnotationTree annotation : annotations) {
            if (annotation.firstToken() != null) {
                return annotation.firstToken().line();
            }
        }
        return -1;
    }

    private static int safeLine(int line) {
        return line < 1 ? -1 : line;
    }

    private static String sourceWindow(List<String> lines, int fromLine, int toLine) {
        int from = Math.max(1, fromLine);
        int to = Math.min(lines.size(), toLine);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i <= to; i++) {
            sb.append(stripTrailingLineComment(lines.get(i - 1))).append('\n');
        }
        return sb.toString();
    }

    /**
     * Removes trailing {@code // ...} comments so CheckVerifier markers
     * ({@code // Noncompliant {{...}}}) do not spoof rollbackFor detection.
     */
    private static String stripTrailingLineComment(String line) {
        boolean inDouble = false;
        boolean inSingle = false;
        int n = line.length();
        for (int i = 0; i < n - 1; i++) {
            char c = line.charAt(i);
            if (!inDouble && !inSingle) {
                if (c == '"') {
                    inDouble = true;
                } else if (c == '\'') {
                    inSingle = true;
                } else if (c == '/' && line.charAt(i + 1) == '/') {
                    return line.substring(0, i);
                }
            } else if (inDouble && c == '"' && !isEscaped(line, i)) {
                inDouble = false;
            } else if (inSingle && c == '\'' && !isEscaped(line, i)) {
                inSingle = false;
            }
        }
        return line;
    }

    private static boolean isEscaped(String line, int quoteIndex) {
        int backslashes = 0;
        for (int j = quoteIndex - 1; j >= 0 && line.charAt(j) == '\\'; j--) {
            backslashes++;
        }
        return (backslashes % 2) == 1;
    }
}
