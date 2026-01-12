grammar Query;

@header {
package dev.snowdrop.mtool.parser.antlr;
}

// Structure of the language
// pom.dependency is (artifactId='quarkus-core', version='3.16.2')
// java.annotation is '@java.lang.SuppressWarnings("deprecation")'
// pom.dependency is (artifactId='quarkus-core', version='3.16.2') OR pom.dependency is (artifactId='quarkus-rest', version='3.16.2')
// pom.dependency is (groupId='io.quarkus', artifactId='quarkus-rest', version='3.16.2') AND java.annotation is '@SpringBootApplication'

searchQuery: operation;
operation
    : operation AND operation #AndOperation
    | operation OR operation #OrOperation
    | clause #SimpleClause
    ;

clause: fileType ('.' symbol)? ('is' | '=') (value | '(' keyValuePair (',' keyValuePair)* ')');
fileType: 'JAVA' | 'java' | 'POM' | 'pom' | 'TEXT' | 'text' | 'PROPERTY' | 'property' | 'PROPERTIES' | 'properties' | 'YAML' | 'yaml' | 'JSON' | 'json';
symbol: ID;
keyValuePair: key '=' value;
key: QUOTED_STRING | ID;
value: QUOTED_STRING | ID;
logicalOp: AND | OR;

// LEXER vocabulary of the language
IS:    'is';
AND:   'AND';
OR:    'OR';

ID:            [a-zA-Z][a-zA-Z0-9-]*; // Identifier, allows dots for package names
QUOTED_STRING: '\'' ( ~('\''|'\\') | '\\' . )* '\''   // Single-quoted string
             | '"' ( ~('"'|'\\') | '\\' . )* '"';     // Double-quoted string
EQUALS:        '=';
DOT:           '.';
COMMA:         ',';
LPAREN:        '(';
RPAREN:        ')';

WS:            [ \t\r\n]+ -> skip; // Skip whitespace