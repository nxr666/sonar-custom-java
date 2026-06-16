package org.sonar.custom.java.xml;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisSelectStarDetectorTest {

    @Test
    void reportsSelectStarInSelect() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<mapper namespace=\"demo\">\n"
                + "  <select id=\"q\">\n"
                + "    SELECT * FROM users\n"
                + "  </select>\n"
                + "</mapper>\n";
        assertThat(MybatisSelectStarDetector.issueLines(xml)).containsExactly(3);
    }

    @Test
    void reportsSelectDistinctStar() {
        String xml = "<mapper namespace=\"demo\"><select id=\"q\">select distinct * from t</select></mapper>";
        assertThat(MybatisSelectStarDetector.issueLines(xml)).containsExactly(1);
    }

    @Test
    void skipsExplicitColumns() {
        String xml = "<mapper namespace=\"demo\"><select id=\"q\">select id, name from t</select></mapper>";
        assertThat(MybatisSelectStarDetector.issueLines(xml)).isEmpty();
    }

    @Test
    void skipsNonMapperXml() {
        String xml = "<project><name>x</name></project>";
        assertThat(MybatisSelectStarDetector.looksLikeMybatisMapper(xml)).isFalse();
        assertThat(MybatisSelectStarDetector.issueLines(xml)).isEmpty();
    }

    @Test
    void countsSqlElementsInDocumentOrderForNthTag() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"ok\">select id from t</select>"
                + "<select id=\"bad\">select * from t</select>"
                + "</mapper>";
        List<Integer> lines = MybatisSelectStarDetector.issueLines(xml);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).isEqualTo(1);
    }

    @Test
    void findNthOpeningTag() {
        String s = "<mapper><select></select><select></select></mapper>";
        assertThat(MybatisSelectStarDetector.findNthOpeningTag(s, "select", 0)).isGreaterThanOrEqualTo(0);
        assertThat(MybatisSelectStarDetector.findNthOpeningTag(s, "select", 1))
                .isGreaterThan(MybatisSelectStarDetector.findNthOpeningTag(s, "select", 0));
    }

    @Test
    void reportsInsertWithoutColumns() {
        String xml = "<mapper namespace=\"demo\">"
                + "<insert id=\"save\">INSERT INTO users VALUES (#{id}, #{name})</insert>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForInsertWithoutColumns(xml)).containsExactly(1);
    }

    @Test
    void skipsInsertWithColumns() {
        String xml = "<mapper namespace=\"demo\">"
                + "<insert id=\"save\">INSERT INTO users (id, name) VALUES (#{id}, #{name})</insert>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForInsertWithoutColumns(xml)).isEmpty();
    }

    @Test
    void reportsInsertWithoutColumnsAcrossLines() {
        String xml = "<mapper namespace=\"demo\">\n"
                + "  <insert id=\"save\">\n"
                + "    INSERT INTO users\n"
                + "    VALUES (#{id}, #{name})\n"
                + "  </insert>\n"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForInsertWithoutColumns(xml)).containsExactly(2);
    }

    @Test
    void reportsPhysicalDelete() {
        String xml = "<mapper namespace=\"demo\">"
                + "<delete id=\"remove\">DELETE FROM users WHERE id = #{id}</delete>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForPhysicalDelete(xml)).containsExactly(1);
    }

    @Test
    void skipsLogicalDeleteUpdate() {
        String xml = "<mapper namespace=\"demo\">"
                + "<update id=\"remove\">UPDATE users SET deleted = 1 WHERE id = #{id}</update>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForPhysicalDelete(xml)).isEmpty();
    }

    @Test
    void reportsUpdateWithoutWhere() {
        String xml = "<mapper namespace=\"demo\">"
                + "<update id=\"disable\">UPDATE users SET enabled = 0</update>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForUpdateOrDeleteWithoutWhere(xml)).containsExactly(1);
    }

    @Test
    void reportsDeleteWithoutWhere() {
        String xml = "<mapper namespace=\"demo\">"
                + "<delete id=\"clear\">DELETE FROM users</delete>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForUpdateOrDeleteWithoutWhere(xml)).containsExactly(1);
    }

    @Test
    void skipsUpdateWithWhereKeyword() {
        String xml = "<mapper namespace=\"demo\">"
                + "<update id=\"disable\">UPDATE users SET enabled = 0 WHERE tenant_id = #{tenantId}</update>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForUpdateOrDeleteWithoutWhere(xml)).isEmpty();
    }

    @Test
    void skipsDeleteWithWhereTag() {
        String xml = "<mapper namespace=\"demo\">"
                + "<delete id=\"clear\">DELETE FROM users <where>id = #{id}</where></delete>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForUpdateOrDeleteWithoutWhere(xml)).isEmpty();
    }

    @Test
    void reportsDeleteWithOnlyConditionalWhereTag() {
        String xml = "<mapper namespace=\"demo\">"
                + "<delete id=\"clear\">DELETE FROM users <where><if test=\"id != null\">id = #{id}</if></where></delete>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForUpdateOrDeleteWithoutWhere(xml)).containsExactly(1);
    }

    @Test
    void reportsUpdateWithConditionalIfWrappingWhere() {
        String xml = "<mapper namespace=\"demo\">"
                + "<update id=\"disable\">UPDATE users SET enabled = 0 "
                + "<if test=\"id != null\">WHERE id = #{id}</if></update>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForUpdateOrDeleteWithoutWhere(xml)).containsExactly(1);
    }

    @Test
    void skipsDeleteWithGuaranteedWhereClauseInWhereTag() {
        String xml = "<mapper namespace=\"demo\">"
                + "<delete id=\"clear\">DELETE FROM users <where>1 = 1 <if test=\"id != null\">AND id = #{id}</if></where></delete>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForUpdateOrDeleteWithoutWhere(xml)).isEmpty();
    }

    @Test
    void skipsSqlStandardFunctionUpper() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT id, UPPER(name) FROM users</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForSqlBuiltinFunctions(xml)).isEmpty();
    }

    @Test
    void skipsSqlStandardFunctionCount() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"c\">SELECT COUNT(*) FROM users</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForSqlBuiltinFunctions(xml)).isEmpty();
    }

    @Test
    void reportsVendorSpecificDateFormat() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT DATE_FORMAT(created_at, '%Y-%m-%d') FROM users</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForSqlBuiltinFunctions(xml)).containsExactly(1);
    }

    @Test
    void reportsVendorSpecificNvl() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT NVL(name, 'N/A') FROM users</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForSqlBuiltinFunctions(xml)).containsExactly(1);
    }

    @Test
    void reportsSpatialStFunction() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"near\">SELECT id FROM places WHERE ST_Distance(loc, #{p}) &lt; 100</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForSqlBuiltinFunctions(xml)).containsExactly(1);
    }

    @Test
    void skipsSqlWithoutFunctions() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT id, name FROM users WHERE id = #{id}</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForSqlBuiltinFunctions(xml)).isEmpty();
    }

    @Test
    void reportsTooManyJoins() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT a.id FROM a "
                + "JOIN b ON a.b_id = b.id "
                + "LEFT JOIN c ON c.a_id = a.id "
                + "INNER JOIN d ON d.c_id = c.id "
                + "JOIN e ON e.d_id = d.id "
                + "LEFT JOIN f ON f.e_id = e.id "
                + "JOIN g ON g.f_id = f.id</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForExcessiveJoins(xml)).containsExactly(1);
        assertThat(MybatisSelectStarDetector.issueLinesForExcessiveNesting(xml)).isEmpty();
    }

    @Test
    void reportsTooManyNestedSubQueries() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT * FROM users u WHERE u.id IN (SELECT user_id FROM orders "
                + "WHERE product_id IN (SELECT id FROM products WHERE supplier_id IN (SELECT id FROM suppliers "
                + "WHERE region_id IN (SELECT id FROM regions WHERE enabled = 1))))</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForExcessiveJoins(xml)).isEmpty();
        assertThat(MybatisSelectStarDetector.issueLinesForExcessiveNesting(xml)).containsExactly(1);
    }

    @Test
    void skipsSimpleJoinCount() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT u.id FROM users u LEFT JOIN dept d ON d.id = u.dept_id "
                + "WHERE u.id IN (SELECT user_id FROM orders)</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForExcessiveJoins(xml)).isEmpty();
    }

    @Test
    void skipsSingleNestedSelect() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT u.id FROM users u WHERE u.id IN (SELECT user_id FROM orders)</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForExcessiveNesting(xml)).isEmpty();
    }

    @Test
    void supportsCustomComplexityThresholds() {
        String joinHeavySql = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT a.id FROM a "
                + "JOIN b ON a.b_id = b.id "
                + "LEFT JOIN c ON c.a_id = a.id "
                + "INNER JOIN d ON d.c_id = c.id</select>"
                + "</mapper>";
        String nestedSql = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT * FROM users u WHERE u.id IN (SELECT user_id FROM orders "
                + "WHERE product_id IN (SELECT id FROM products WHERE deleted = 0))</select>"
                + "</mapper>";

        assertThat(MybatisSelectStarDetector.issueLinesForExcessiveJoins(joinHeavySql, 3)).isEmpty();
        assertThat(MybatisSelectStarDetector.issueLinesForExcessiveNesting(nestedSql, 2)).isEmpty();
    }

    @Test
    void skipsModerateComplexityWithNewDefaults() {
        String joinSql = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT a.id FROM a "
                + "JOIN b ON a.b_id = b.id "
                + "LEFT JOIN c ON c.a_id = a.id "
                + "INNER JOIN d ON d.c_id = c.id</select>"
                + "</mapper>";
        String nestingSql = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT * FROM users u WHERE u.id IN (SELECT user_id FROM orders "
                + "WHERE product_id IN (SELECT id FROM products WHERE deleted = 0))</select>"
                + "</mapper>";

        assertThat(MybatisSelectStarDetector.issueLinesForExcessiveJoins(joinSql)).isEmpty();
        assertThat(MybatisSelectStarDetector.issueLinesForExcessiveNesting(nestingSql)).isEmpty();
    }

    @Test
    void reportsOneEqualsOneCondition() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT id FROM users WHERE 1=1 AND id = #{id}</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForOneEqualsOneCondition(xml)).containsExactly(1);
    }

    @Test
    void reportsOneEqualsOneConditionWithWhitespace() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT id FROM users WHERE 1 = 1</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForOneEqualsOneCondition(xml)).containsExactly(1);
    }

    @Test
    void skipsSqlWithoutOneEqualsOneCondition() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT id FROM users WHERE enabled = 1</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForOneEqualsOneCondition(xml)).isEmpty();
    }

    @Test
    void reportsDollarPlaceholderInSelect() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT id FROM users WHERE name = ${name}</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForSqlInjectionRisk(xml)).containsExactly(1);
    }

    @Test
    void reportsDollarPlaceholderInOrderBy() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT id FROM users ORDER BY ${sortColumn}</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForSqlInjectionRisk(xml)).containsExactly(1);
    }

    @Test
    void skipsSafeHashPlaceholder() {
        String xml = "<mapper namespace=\"demo\">"
                + "<select id=\"q\">SELECT id FROM users WHERE name = #{name}</select>"
                + "</mapper>";
        assertThat(MybatisSelectStarDetector.issueLinesForSqlInjectionRisk(xml)).isEmpty();
    }
}
