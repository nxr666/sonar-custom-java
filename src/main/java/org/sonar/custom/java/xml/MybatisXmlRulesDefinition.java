package org.sonar.custom.java.xml;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

import java.util.Arrays;

/**
 * MyBatis mapper XML rules (language {@code xml}).
 */
public class MybatisXmlRulesDefinition implements RulesDefinition {

    public static final String REPOSITORY_KEY = "custom-mybatis-xml";
    public static final String REPOSITORY_NAME = "Custom MyBatis XML Rules";
    public static final String RULE_AVOID_SELECT_STAR = "AvoidSelectStarInMybatisMapper";
    public static final String RULE_AVOID_INSERT_WITHOUT_COLUMNS = "AvoidInsertWithoutColumnsInMybatisMapper";
    public static final String RULE_AVOID_PHYSICAL_DELETE = "AvoidPhysicalDeleteInMybatisMapper";
    public static final String RULE_AVOID_UPDATE_OR_DELETE_WITHOUT_WHERE = "AvoidUpdateOrDeleteWithoutWhereInMybatisMapper";
    public static final String RULE_AVOID_SQL_BUILTIN_FUNCTIONS = "AvoidSqlBuiltinFunctionsInMybatisMapper";
    public static final String RULE_AVOID_EXCESSIVE_JOINS = "AvoidExcessiveJoinsInMybatisMapper";
    public static final String RULE_AVOID_EXCESSIVE_NESTING = "AvoidExcessiveNestingInMybatisMapper";
    public static final String RULE_AVOID_ONE_EQUALS_ONE_CONDITION = "AvoidOneEqualsOneConditionInMybatisMapper";
    public static final String RULE_AVOID_SQL_INJECTION_RISK = "AvoidSqlInjectionRiskInMybatisMapper";

    private static final String RESOURCE_FOLDER = "org/sonar/l10n/xml/rules/custom";

    @Override
    public void define(Context context) {
        NewRepository repository = context.createRepository(REPOSITORY_KEY, "xml")
                .setName(REPOSITORY_NAME);

        RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER);
        ruleMetadataLoader.addRulesByRuleKey(repository, Arrays.asList(
                RULE_AVOID_SELECT_STAR,
                RULE_AVOID_INSERT_WITHOUT_COLUMNS,
                RULE_AVOID_PHYSICAL_DELETE,
                RULE_AVOID_UPDATE_OR_DELETE_WITHOUT_WHERE,
                RULE_AVOID_SQL_BUILTIN_FUNCTIONS,
                RULE_AVOID_EXCESSIVE_JOINS,
                RULE_AVOID_EXCESSIVE_NESTING,
                RULE_AVOID_ONE_EQUALS_ONE_CONDITION,
                RULE_AVOID_SQL_INJECTION_RISK
        ));

        repository.done();
    }
}
