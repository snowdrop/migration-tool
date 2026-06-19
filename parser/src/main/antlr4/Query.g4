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

// OLD : clause: fileType ('.' symbol)? ('is' | '=' | 'all') (value | '(' keyValuePair (',' keyValuePair)* ')');
// New clause syntax supporting to search about: find all java.classes
clause
    : FIND? 'all'? fileType (DOT symbol)? (assignmentOp valueOrPairs)?
    ;

// Separated sub-rules to keep the AST syntax tree nodes clean
assignmentOp: 'is' | '=' | 'all';
valueOrPairs: value | LPAREN keyValuePair (COMMA keyValuePair)* RPAREN;

fileType: 'JAVA' | 'java' | 'POM' | 'pom' | 'TEXT' | 'text' | 'PROPERTY' | 'property' | 'PROPERTIES' | 'properties' | 'YAML' | 'yaml' | 'JSON' | 'json';
symbol: ID;
keyValuePair: key EQUALS value;
key: QUOTED_STRING | ID;
value: QUOTED_STRING | ID;

// LEXER vocabulary of the language
FIND:  'FIND' | 'find'; // Added explicit support for your action verb
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