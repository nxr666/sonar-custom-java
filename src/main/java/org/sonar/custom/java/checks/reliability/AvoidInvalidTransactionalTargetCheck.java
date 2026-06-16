package org.sonar.custom.java.checks.reliability;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "AvoidInvalidTransactionalTarget")
public class AvoidInvalidTransactionalTargetCheck extends IssuableSubscriptionVisitor {

    private static final String MSG_METHOD =
            "@Transactional on a %s method has no effect; Spring proxy cannot intercept it.";
    private static final String MSG_FINAL_CLASS =
            "@Transactional on a final class has no effect; CGLIB cannot subclass it.";

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
        ClassTree classTree = (ClassTree) tree;

        if (hasTransactional(classTree.modifiers()) && hasModifier(classTree.modifiers(), Modifier.FINAL)) {
            reportIssue(classTree, MSG_FINAL_CLASS);
        }

        for (Tree member : classTree.members()) {
            if (!member.is(Tree.Kind.METHOD)) {
                continue;
            }
            MethodTree method = (MethodTree) member;
            if (!hasTransactional(method.modifiers())) {
                continue;
            }
            String invalid = findInvalidModifier(method.modifiers());
            if (invalid != null) {
                reportIssue(method.simpleName(), String.format(MSG_METHOD, invalid));
            }
        }
    }

    private static boolean hasTransactional(ModifiersTree modifiers) {
        for (AnnotationTree annotation : modifiers.annotations()) {
            String name = annotation.annotationType().toString();
            if ("Transactional".equals(name) || name.endsWith(".Transactional")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasModifier(ModifiersTree modifiers, Modifier target) {
        for (ModifierKeywordTree kw : modifiers.modifiers()) {
            if (kw.modifier() == target) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the first invalid modifier name for a @Transactional method, or null if none.
     * private/static/final are always ineffective under proxy-based AOP.
     * protected is excluded: CGLIB can proxy protected methods.
     */
    private static String findInvalidModifier(ModifiersTree modifiers) {
        for (ModifierKeywordTree kw : modifiers.modifiers()) {
            switch (kw.modifier()) {
                case PRIVATE: return "private";
                case STATIC:  return "static";
                case FINAL:   return "final";
                default: break;
            }
        }
        return null;
    }
}
