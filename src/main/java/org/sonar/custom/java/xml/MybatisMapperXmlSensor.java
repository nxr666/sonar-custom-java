package org.sonar.custom.java.xml;

import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans MyBatis mapper XML for SQL bad practices.
 */
public class MybatisMapperXmlSensor implements Sensor {

    /**
     * Complexity thresholds for mapper SQL.
     * Adjust here when the team needs stricter/looser limits.
     */
    private static final int MAX_JOIN_COUNT = 5;
    private static final int MAX_NESTED_SELECT_COUNT = 3;

    private static final String SELECT_STAR_MESSAGE =
            "Avoid SELECT * in MyBatis SQL; list explicit columns for maintainability, stable contracts, and performance.";
    private static final String INSERT_WITHOUT_COLUMNS_MESSAGE =
            "Avoid INSERT INTO ... VALUES without explicit column names in MyBatis SQL; always specify the target columns.";
    private static final String PHYSICAL_DELETE_MESSAGE =
            "Avoid physical delete (DELETE FROM) in MyBatis SQL; prefer logical delete (for example update deleted flag).";
    private static final String UPDATE_OR_DELETE_WITHOUT_WHERE_MESSAGE =
            "Avoid UPDATE/DELETE without WHERE condition in MyBatis SQL to prevent full-table changes.";
    private static final String SQL_BUILTIN_FUNCTION_MESSAGE =
            "Avoid non-standard SQL functions in MyBatis SQL; database vendors differ, so implement formatting, parsing, "
                    + "and calculations in Java for migration-friendly SQL.";
    private static final String EXCESSIVE_JOINS_MESSAGE =
            "Avoid too many JOINs in MyBatis SQL; excessive joins reduce readability and increase execution risk.";
    private static final String EXCESSIVE_NESTING_MESSAGE =
            "Avoid deeply nested subqueries in MyBatis SQL; excessive nesting hurts readability and maintainability.";
    private static final String ONE_EQUALS_ONE_CONDITION_MESSAGE =
            "Avoid using 1=1 as a query condition in MyBatis SQL; build conditions explicitly for clarity and safety.";
    private static final String SQL_INJECTION_RISK_MESSAGE =
            "Avoid using ${...} in MyBatis SQL; prefer #{...} parameter binding to reduce SQL injection risk.";

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name("Custom MyBatis mapper XML");
        descriptor.createIssuesForRuleRepositories(MybatisXmlRulesDefinition.REPOSITORY_KEY);
    }

    @Override
    public void execute(SensorContext context) {
        RuleKey selectStarRuleKey = RuleKey.of(
                MybatisXmlRulesDefinition.REPOSITORY_KEY,
                MybatisXmlRulesDefinition.RULE_AVOID_SELECT_STAR
        );
        RuleKey insertWithoutColumnsRuleKey = RuleKey.of(
                MybatisXmlRulesDefinition.REPOSITORY_KEY,
                MybatisXmlRulesDefinition.RULE_AVOID_INSERT_WITHOUT_COLUMNS
        );
        RuleKey physicalDeleteRuleKey = RuleKey.of(
                MybatisXmlRulesDefinition.REPOSITORY_KEY,
                MybatisXmlRulesDefinition.RULE_AVOID_PHYSICAL_DELETE
        );
        RuleKey updateOrDeleteWithoutWhereRuleKey = RuleKey.of(
                MybatisXmlRulesDefinition.REPOSITORY_KEY,
                MybatisXmlRulesDefinition.RULE_AVOID_UPDATE_OR_DELETE_WITHOUT_WHERE
        );
        RuleKey sqlBuiltinFunctionsRuleKey = RuleKey.of(
                MybatisXmlRulesDefinition.REPOSITORY_KEY,
                MybatisXmlRulesDefinition.RULE_AVOID_SQL_BUILTIN_FUNCTIONS
        );
        RuleKey excessiveJoinsRuleKey = RuleKey.of(
                MybatisXmlRulesDefinition.REPOSITORY_KEY,
                MybatisXmlRulesDefinition.RULE_AVOID_EXCESSIVE_JOINS
        );
        RuleKey excessiveNestingRuleKey = RuleKey.of(
                MybatisXmlRulesDefinition.REPOSITORY_KEY,
                MybatisXmlRulesDefinition.RULE_AVOID_EXCESSIVE_NESTING
        );
        RuleKey oneEqualsOneConditionRuleKey = RuleKey.of(
                MybatisXmlRulesDefinition.REPOSITORY_KEY,
                MybatisXmlRulesDefinition.RULE_AVOID_ONE_EQUALS_ONE_CONDITION
        );
        RuleKey sqlInjectionRiskRuleKey = RuleKey.of(
                MybatisXmlRulesDefinition.REPOSITORY_KEY,
                MybatisXmlRulesDefinition.RULE_AVOID_SQL_INJECTION_RISK
        );
        boolean selectStarActive = context.activeRules().find(selectStarRuleKey) != null;
        boolean insertWithoutColumnsActive = context.activeRules().find(insertWithoutColumnsRuleKey) != null;
        boolean physicalDeleteActive = context.activeRules().find(physicalDeleteRuleKey) != null;
        boolean updateOrDeleteWithoutWhereActive = context.activeRules().find(updateOrDeleteWithoutWhereRuleKey) != null;
        boolean sqlBuiltinFunctionsActive = context.activeRules().find(sqlBuiltinFunctionsRuleKey) != null;
        boolean excessiveJoinsActive = context.activeRules().find(excessiveJoinsRuleKey) != null;
        boolean excessiveNestingActive = context.activeRules().find(excessiveNestingRuleKey) != null;
        boolean oneEqualsOneConditionActive = context.activeRules().find(oneEqualsOneConditionRuleKey) != null;
        boolean sqlInjectionRiskActive = context.activeRules().find(sqlInjectionRiskRuleKey) != null;
        if (!selectStarActive && !insertWithoutColumnsActive && !physicalDeleteActive && !updateOrDeleteWithoutWhereActive
                && !sqlBuiltinFunctionsActive && !excessiveJoinsActive && !excessiveNestingActive
                && !oneEqualsOneConditionActive && !sqlInjectionRiskActive) {
            return;
        }

        FilePredicates p = context.fileSystem().predicates();
        Iterable<InputFile> xmlFiles = context.fileSystem().inputFiles(
                p.and(
                        p.hasExtension("xml"),
                        p.hasType(InputFile.Type.MAIN)
                ));

        for (InputFile inputFile : xmlFiles) {
            processFile(
                    context,
                    inputFile,
                    selectStarActive,
                    selectStarRuleKey,
                    insertWithoutColumnsActive,
                    insertWithoutColumnsRuleKey,
                    physicalDeleteActive,
                    physicalDeleteRuleKey,
                    updateOrDeleteWithoutWhereActive,
                    updateOrDeleteWithoutWhereRuleKey,
                    sqlBuiltinFunctionsActive,
                    sqlBuiltinFunctionsRuleKey,
                    excessiveJoinsActive,
                    excessiveJoinsRuleKey,
                    excessiveNestingActive,
                    excessiveNestingRuleKey,
                    oneEqualsOneConditionActive,
                    oneEqualsOneConditionRuleKey,
                    sqlInjectionRiskActive,
                    sqlInjectionRiskRuleKey
            );
        }
    }

    private static void processFile(
            SensorContext context,
            InputFile inputFile,
            boolean selectStarActive,
            RuleKey selectStarRuleKey,
            boolean insertWithoutColumnsActive,
            RuleKey insertWithoutColumnsRuleKey,
            boolean physicalDeleteActive,
            RuleKey physicalDeleteRuleKey,
            boolean updateOrDeleteWithoutWhereActive,
            RuleKey updateOrDeleteWithoutWhereRuleKey,
            boolean sqlBuiltinFunctionsActive,
            RuleKey sqlBuiltinFunctionsRuleKey,
            boolean excessiveJoinsActive,
            RuleKey excessiveJoinsRuleKey,
            boolean excessiveNestingActive,
            RuleKey excessiveNestingRuleKey,
            boolean oneEqualsOneConditionActive,
            RuleKey oneEqualsOneConditionRuleKey,
            boolean sqlInjectionRiskActive,
            RuleKey sqlInjectionRiskRuleKey
    ) {
        String content;
        try {
            content = inputFile.contents();
        } catch (IOException e) {
            return;
        }
        if (!MybatisSelectStarDetector.looksLikeMybatisMapper(content)) {
            return;
        }

        if (selectStarActive) {
            List<Integer> lines = MybatisSelectStarDetector.issueLinesForSelectStar(content);
            reportIssues(context, inputFile, selectStarRuleKey, SELECT_STAR_MESSAGE, lines);
        }
        if (insertWithoutColumnsActive) {
            List<Integer> lines = MybatisSelectStarDetector.issueLinesForInsertWithoutColumns(content);
            reportIssues(context, inputFile, insertWithoutColumnsRuleKey, INSERT_WITHOUT_COLUMNS_MESSAGE, lines);
        }
        if (physicalDeleteActive) {
            List<Integer> lines = MybatisSelectStarDetector.issueLinesForPhysicalDelete(content);
            reportIssues(context, inputFile, physicalDeleteRuleKey, PHYSICAL_DELETE_MESSAGE, lines);
        }
        if (updateOrDeleteWithoutWhereActive) {
            List<Integer> lines = MybatisSelectStarDetector.issueLinesForUpdateOrDeleteWithoutWhere(content);
            reportIssues(context, inputFile, updateOrDeleteWithoutWhereRuleKey, UPDATE_OR_DELETE_WITHOUT_WHERE_MESSAGE, lines);
        }
        if (sqlBuiltinFunctionsActive) {
            List<Integer> lines = MybatisSelectStarDetector.issueLinesForSqlBuiltinFunctions(content);
            reportIssues(context, inputFile, sqlBuiltinFunctionsRuleKey, SQL_BUILTIN_FUNCTION_MESSAGE, lines);
        }
        if (excessiveJoinsActive) {
            List<Integer> lines = MybatisSelectStarDetector.issueLinesForExcessiveJoins(content, MAX_JOIN_COUNT);
            reportIssues(context, inputFile, excessiveJoinsRuleKey, EXCESSIVE_JOINS_MESSAGE, lines);
        }
        if (excessiveNestingActive) {
            List<Integer> lines = MybatisSelectStarDetector.issueLinesForExcessiveNesting(content, MAX_NESTED_SELECT_COUNT);
            reportIssues(context, inputFile, excessiveNestingRuleKey, EXCESSIVE_NESTING_MESSAGE, lines);
        }
        if (oneEqualsOneConditionActive) {
            List<Integer> lines = MybatisSelectStarDetector.issueLinesForOneEqualsOneCondition(content);
            reportIssues(context, inputFile, oneEqualsOneConditionRuleKey, ONE_EQUALS_ONE_CONDITION_MESSAGE, lines);
        }
        if (sqlInjectionRiskActive) {
            List<Integer> lines = MybatisSelectStarDetector.issueLinesForSqlInjectionRisk(content);
            reportIssues(context, inputFile, sqlInjectionRiskRuleKey, SQL_INJECTION_RISK_MESSAGE, lines);
        }
    }

    private static void reportIssues(
            SensorContext context,
            InputFile inputFile,
            RuleKey ruleKey,
            String message,
            List<Integer> lines
    ) {
        Set<Integer> uniqueLines = new LinkedHashSet<>(lines);
        for (Integer line : uniqueLines) {
            NewIssue issue = context.newIssue();
            NewIssueLocation location = issue.newLocation()
                    .on(inputFile)
                    .at(inputFile.selectLine(line))
                    .message(message);
            issue.forRule(ruleKey).at(location).save();
        }
    }
}
