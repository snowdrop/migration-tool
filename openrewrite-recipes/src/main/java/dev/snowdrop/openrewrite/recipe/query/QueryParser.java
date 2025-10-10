package dev.snowdrop.openrewrite.recipe.query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryParser {
    private final List<String> tokens;
    private int pos = 0;

    private QueryParser(String conditionsStr) {
        this.tokens = tokenize(conditionsStr);
    }

    public static Condition parse(String conditionsStr) {
        return new QueryParser(conditionsStr).parseExpression();
    }

    private Condition parseExpression() {
        Condition left = parseAnd();
        while (pos < tokens.size() && "OR".equalsIgnoreCase(tokens.get(pos))) {
            pos++;
            Condition right = parseAnd();
            left = new OrCondition(left, right);
        }
        return left;
    }

    private Condition parseAnd() {
        Condition left = parsePrimary();
        while (pos < tokens.size() && ("AND".equalsIgnoreCase(tokens.get(pos)) || ",".equals(tokens.get(pos)))) {
            pos++;
            Condition right = parsePrimary();
            left = new AndCondition(left, right);
        }
        return left;
    }

    private Condition parsePrimary() {
        if ("(".equals(tokens.get(pos))) {
            pos++; // Consume '('
            Condition expr = parseExpression();
            pos++; // Consume ')'
            return expr;
        }
        // It's a simple condition: key, operator, value
        String key = tokens.get(pos++);
        String op = tokens.get(pos++);
        String val = tokens.get(pos++).replaceAll("'", "");
        return new SimpleCondition(key, op, val);
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        // This regex now handles: 'quoted values', unquoted-values-with-dots, operators, parens, and commas
        Pattern pattern = Pattern.compile("'[^']*'|[\\w.-]+|[()=,]");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }
}