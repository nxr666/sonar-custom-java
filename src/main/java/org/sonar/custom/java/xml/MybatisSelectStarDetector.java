package org.sonar.custom.java.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects {@code SELECT *} (and {@code SELECT DISTINCT *}) in MyBatis mapper XML SQL bodies.
 */
public final class MybatisSelectStarDetector {

    private static final Pattern SELECT_STAR = Pattern.compile("\\bselect\\s+(?:distinct\\s+)?\\*", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_WITHOUT_COLUMNS = Pattern.compile(
            "\\binsert\\s+into\\s+(?:`[^`]+`|\"[^\"]+\"|\\[[^\\]]+\\]|[\\w$.]+)\\s+values\\s*\\(",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PHYSICAL_DELETE = Pattern.compile(
            "\\bdelete\\s+from\\s+(?:`[^`]+`|\"[^\"]+\"|\\[[^\\]]+\\]|[\\w$.]+)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern UPDATE_START = Pattern.compile(
            "\\bupdate\\s+(?:`[^`]+`|\"[^\"]+\"|\\[[^\\]]+\\]|[\\w$.]+)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DELETE_START = Pattern.compile(
            "\\bdelete\\s+from\\s+(?:`[^`]+`|\"[^\"]+\"|\\[[^\\]]+\\]|[\\w$.]+)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ONE_EQUALS_ONE_CONDITION = Pattern.compile("\\b1\\s*=\\s*1\\b");
    private static final Pattern DOLLAR_PLACEHOLDER = Pattern.compile("\\$\\{[^}]+\\}");
    private static final Pattern WHERE_WORD = Pattern.compile("\\bwhere\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAPPER_OPEN = Pattern.compile("<mapper\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN_KEYWORD = Pattern.compile(
            "\\b(?:(?:inner|left(?:\\s+outer)?|right(?:\\s+outer)?|full(?:\\s+outer)?|cross)\\s+)?join\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NESTED_SELECT = Pattern.compile("\\(\\s*select\\b", Pattern.CASE_INSENSITIVE);
    private static final int DEFAULT_MAX_JOIN_COUNT = 5;
    private static final int DEFAULT_MAX_NESTED_SELECT_COUNT = 3;
    /**
     * Non-standard (vendor-specific) SQL functions and spatial {@code ST_*} calls.
     * SQL standard functions are intentionally excluded.
     */
    private static final Pattern SQL_BUILTIN_FUNCTION_CALL = buildSqlBuiltinFunctionPattern();

    /**
     * SQL-bearing child elements of {@code <mapper>}.
     */
    private static final String[] SQL_ELEMENT_TAGS = {"select", "insert", "update", "delete", "sql"};

    private MybatisSelectStarDetector() {
    }

    public static boolean looksLikeMybatisMapper(String content) {
        return content != null && MAPPER_OPEN.matcher(content).find();
    }

    public static List<Integer> issueLines(String content) {
        return issueLinesForSelectStar(content);
    }

    public static List<Integer> issueLinesForSelectStar(String content) {
        return issueLinesMatching(content, SELECT_STAR);
    }

    public static List<Integer> issueLinesForInsertWithoutColumns(String content) {
        return issueLinesMatching(content, INSERT_WITHOUT_COLUMNS);
    }

    public static List<Integer> issueLinesForPhysicalDelete(String content) {
        return issueLinesMatching(content, PHYSICAL_DELETE);
    }

    public static List<Integer> issueLinesForSqlBuiltinFunctions(String content) {
        return issueLinesMatching(content, SQL_BUILTIN_FUNCTION_CALL);
    }

    public static List<Integer> issueLinesForOneEqualsOneCondition(String content) {
        return issueLinesMatching(content, ONE_EQUALS_ONE_CONDITION);
    }

    public static List<Integer> issueLinesForSqlInjectionRisk(String content) {
        return issueLinesMatching(content, DOLLAR_PLACEHOLDER);
    }

    public static List<Integer> issueLinesForExcessiveJoins(String content) {
        return issueLinesForExcessiveJoins(content, DEFAULT_MAX_JOIN_COUNT);
    }

    public static List<Integer> issueLinesForExcessiveJoins(String content, int maxJoinCount) {
        if (content == null || content.isEmpty() || !looksLikeMybatisMapper(content)) {
            return Collections.emptyList();
        }
        Document doc;
        try {
            doc = parseXml(content);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
        Element root = doc.getDocumentElement();
        if (root == null) {
            return Collections.emptyList();
        }

        Map<String, Integer> occurrence = new HashMap<>();
        List<Integer> lines = new ArrayList<>();
        visitForExcessiveJoins(root, occurrence, content, lines, maxJoinCount);
        return lines;
    }

    public static List<Integer> issueLinesForExcessiveNesting(String content) {
        return issueLinesForExcessiveNesting(content, DEFAULT_MAX_NESTED_SELECT_COUNT);
    }

    public static List<Integer> issueLinesForExcessiveNesting(String content, int maxNestedSelectCount) {
        if (content == null || content.isEmpty() || !looksLikeMybatisMapper(content)) {
            return Collections.emptyList();
        }
        Document doc;
        try {
            doc = parseXml(content);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
        Element root = doc.getDocumentElement();
        if (root == null) {
            return Collections.emptyList();
        }

        Map<String, Integer> occurrence = new HashMap<>();
        List<Integer> lines = new ArrayList<>();
        visitForExcessiveNesting(root, occurrence, content, lines, maxNestedSelectCount);
        return lines;
    }

    public static List<Integer> issueLinesForUpdateOrDeleteWithoutWhere(String content) {
        if (content == null || content.isEmpty() || !looksLikeMybatisMapper(content)) {
            return Collections.emptyList();
        }
        Document doc;
        try {
            doc = parseXml(content);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
        Element root = doc.getDocumentElement();
        if (root == null) {
            return Collections.emptyList();
        }

        Map<String, Integer> occurrence = new HashMap<>();
        List<Integer> lines = new ArrayList<>();
        visitForUpdateOrDeleteWithoutWhere(root, occurrence, content, lines);
        return lines;
    }

    private static List<Integer> issueLinesMatching(String content, Pattern sqlPattern) {
        if (content == null || content.isEmpty() || !looksLikeMybatisMapper(content)) {
            return Collections.emptyList();
        }
        Document doc;
        try {
            doc = parseXml(content);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
        Element root = doc.getDocumentElement();
        if (root == null) {
            return Collections.emptyList();
        }

        Map<String, Integer> occurrence = new HashMap<>();
        List<Integer> lines = new ArrayList<>();
        visitForSqlElements(root, occurrence, content, sqlPattern, lines);
        return lines;
    }

    private static void visitForSqlElements(Element element, Map<String, Integer> occurrence, String raw, Pattern sqlPattern, List<Integer> lines) {
        if (isSqlElement(element)) {
            String tag = elementLocalName(element);
            int indexOneBased = occurrence.merge(tag, 1, Integer::sum);
            String sqlText = element.getTextContent();
            if (sqlText != null && sqlPattern.matcher(sqlText).find()) {
                int tagStart = findNthOpeningTag(raw, tag, indexOneBased - 1);
                if (tagStart >= 0) {
                    lines.add(lineNumberAtIndex(raw, tagStart));
                }
            }
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element) {
                visitForSqlElements((Element) n, occurrence, raw, sqlPattern, lines);
            }
        }
    }

    private static void visitForUpdateOrDeleteWithoutWhere(Element element, Map<String, Integer> occurrence, String raw, List<Integer> lines) {
        if (isSqlElement(element)) {
            String tag = elementLocalName(element);
            int indexOneBased = occurrence.merge(tag, 1, Integer::sum);
            String sqlText = element.getTextContent();
            if (isUpdateOrDeleteStatement(sqlText) && !hasWhereCondition(element)) {
                int tagStart = findNthOpeningTag(raw, tag, indexOneBased - 1);
                if (tagStart >= 0) {
                    lines.add(lineNumberAtIndex(raw, tagStart));
                }
            }
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element) {
                visitForUpdateOrDeleteWithoutWhere((Element) n, occurrence, raw, lines);
            }
        }
    }

    private static void visitForExcessiveJoins(
            Element element,
            Map<String, Integer> occurrence,
            String raw,
            List<Integer> lines,
            int maxJoinCount
    ) {
        if (isSqlElement(element)) {
            String tag = elementLocalName(element);
            int indexOneBased = occurrence.merge(tag, 1, Integer::sum);
            String sqlText = element.getTextContent();
            if (isExcessiveJoins(sqlText, maxJoinCount)) {
                int tagStart = findNthOpeningTag(raw, tag, indexOneBased - 1);
                if (tagStart >= 0) {
                    lines.add(lineNumberAtIndex(raw, tagStart));
                }
            }
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element) {
                visitForExcessiveJoins((Element) n, occurrence, raw, lines, maxJoinCount);
            }
        }
    }

    private static void visitForExcessiveNesting(
            Element element,
            Map<String, Integer> occurrence,
            String raw,
            List<Integer> lines,
            int maxNestedSelectCount
    ) {
        if (isSqlElement(element)) {
            String tag = elementLocalName(element);
            int indexOneBased = occurrence.merge(tag, 1, Integer::sum);
            String sqlText = element.getTextContent();
            if (isExcessiveNesting(sqlText, maxNestedSelectCount)) {
                int tagStart = findNthOpeningTag(raw, tag, indexOneBased - 1);
                if (tagStart >= 0) {
                    lines.add(lineNumberAtIndex(raw, tagStart));
                }
            }
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element) {
                visitForExcessiveNesting((Element) n, occurrence, raw, lines, maxNestedSelectCount);
            }
        }
    }

    private static boolean isExcessiveJoins(String sqlText, int maxJoinCount) {
        if (sqlText == null) {
            return false;
        }
        int joinCount = countMatches(JOIN_KEYWORD, sqlText);
        return joinCount > Math.max(0, maxJoinCount);
    }

    private static boolean isExcessiveNesting(String sqlText, int maxNestedSelectCount) {
        if (sqlText == null) {
            return false;
        }
        int nestedSelectCount = countMatches(NESTED_SELECT, sqlText);
        return nestedSelectCount > Math.max(0, maxNestedSelectCount);
    }

    private static int countMatches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static boolean isUpdateOrDeleteStatement(String sqlText) {
        if (sqlText == null) {
            return false;
        }
        return UPDATE_START.matcher(sqlText).find() || DELETE_START.matcher(sqlText).find();
    }

    private static boolean hasWhereCondition(Element sqlElement) {
        if (hasUnconditionalWhereKeyword(sqlElement, false)) {
            return true;
        }
        return hasGuaranteedWhereTagCondition(sqlElement);
    }

    private static boolean hasGuaranteedWhereTagCondition(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element) {
                Element child = (Element) n;
                String name = elementLocalName(child);
                if ("where".equalsIgnoreCase(name)) {
                    if (hasGuaranteedClause(child, false)) {
                        return true;
                    }
                    continue;
                }
                if (hasGuaranteedWhereTagCondition(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasUnconditionalWhereKeyword(Node node, boolean conditionalContext) {
        if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
            String text = node.getTextContent();
            return !conditionalContext && text != null && WHERE_WORD.matcher(text).find();
        }
        if (!(node instanceof Element)) {
            return false;
        }

        Element element = (Element) node;
        String name = elementLocalName(element);
        if ("if".equalsIgnoreCase(name) || "when".equalsIgnoreCase(name)) {
            return hasUnconditionalWhereInChildren(element, true);
        }
        if ("choose".equalsIgnoreCase(name)) {
            if (hasUnconditionalWhereInOtherwise(element, conditionalContext)) {
                return true;
            }
            return hasUnconditionalWhereInChildren(element, conditionalContext);
        }
        return hasUnconditionalWhereInChildren(element, conditionalContext);
    }

    private static boolean hasUnconditionalWhereInChildren(Element element, boolean conditionalContext) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (hasUnconditionalWhereKeyword(children.item(i), conditionalContext)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasUnconditionalWhereInOtherwise(Element chooseElement, boolean conditionalContext) {
        NodeList children = chooseElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element && "otherwise".equalsIgnoreCase(elementLocalName((Element) node))) {
                if (hasUnconditionalWhereInChildren((Element) node, conditionalContext)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasGuaranteedClause(Node node, boolean conditionalContext) {
        if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
            String text = node.getTextContent();
            return !conditionalContext && text != null && !text.trim().isEmpty();
        }
        if (!(node instanceof Element)) {
            return false;
        }

        Element element = (Element) node;
        String name = elementLocalName(element);
        if ("if".equalsIgnoreCase(name) || "when".equalsIgnoreCase(name)) {
            return hasGuaranteedClauseInChildren(element, true);
        }
        if ("choose".equalsIgnoreCase(name)) {
            return hasGuaranteedChooseClause(element, conditionalContext);
        }
        return hasGuaranteedClauseInChildren(element, conditionalContext);
    }

    private static boolean hasGuaranteedClauseInChildren(Element element, boolean conditionalContext) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (hasGuaranteedClause(children.item(i), conditionalContext)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasGuaranteedChooseClause(Element chooseElement, boolean conditionalContext) {
        NodeList children = chooseElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element && "otherwise".equalsIgnoreCase(elementLocalName((Element) node))) {
                return hasGuaranteedClauseInChildren((Element) node, conditionalContext);
            }
        }
        return false;
    }

    private static boolean isSqlElement(Element el) {
        String name = elementLocalName(el);
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String tag : SQL_ELEMENT_TAGS) {
            if (tag.equals(lower)) {
                return true;
            }
        }
        return false;
    }

    private static String elementLocalName(Element el) {
        if (el.getLocalName() != null && !el.getLocalName().isEmpty()) {
            return el.getLocalName();
        }
        String tag = el.getTagName();
        if (tag == null) {
            return null;
        }
        int colon = tag.indexOf(':');
        return colon >= 0 ? tag.substring(colon + 1) : tag;
    }

    /**
     * {@code occurrenceIndex} is 0-based.
     */
    static int findNthOpeningTag(String content, String tagName, int occurrenceIndex) {
        Pattern p = Pattern.compile("<" + Pattern.quote(tagName) + "\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(content);
        int seen = 0;
        while (m.find()) {
            if (seen == occurrenceIndex) {
                return m.start();
            }
            seen++;
        }
        return -1;
    }

    static int lineNumberAtIndex(String content, int index) {
        int line = 1;
        int limit = Math.min(index, content.length());
        for (int i = 0; i < limit; i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static Pattern buildSqlBuiltinFunctionPattern() {
        String[] names = {
                // MySQL / MariaDB specific
                "ifnull", "str_to_date", "date_format", "from_unixtime", "unix_timestamp", "timestampdiff",
                "timestampadd", "group_concat", "substring_index", "extractvalue", "updatexml",
                // Oracle specific
                "nvl", "nvl2", "decode", "to_char", "to_date", "to_number", "to_timestamp", "add_months",
                "months_between", "next_day", "sysdate", "systimestamp", "wm_concat",
                // PostgreSQL specific
                "split_part", "strpos", "jsonb_extract_path", "jsonb_set",
                // SQL Server specific
                "isnull", "dateadd", "datediff", "datepart", "getdate", "getutcdate", "sysdatetime",
                "sysutcdatetime", "switchoffset", "todatetimeoffset",
                // Cross-vendor but still non-standard in details
                "regexp_like", "regexp_replace", "regexp_substr", "regexp_instr"
        };
        StringBuilder sb = new StringBuilder();
        sb.append("\\b(?:");
        for (int i = 0; i < names.length; i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(Pattern.quote(names[i]));
        }
        sb.append("|st_[a-z0-9_]+)\\s*\\(");
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }

    private static Document parseXml(String content) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setExpandEntityReferences(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        return db.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
