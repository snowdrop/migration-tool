grammar Query;

@header {
package dev.snowdrop.parser.antlr;
}

// Structure of the language
// FIND pom.dependency WHERE (artifactId='quarkus-core', version='3.16.2')
// FIND pom.dependency WHERE (artifactId='quarkus-core', version='3.16.2') OR pom.dependency WHERE (artifactId='quarkus-rest', version='3.16.2')
// FIND pom.dependency WHERE (artifactId='spring-boot', version='3.5.6') AND java.annotation WHERE (name='@SpringBootApplication')

searchQuery: 'FIND' operation;
operation
    : operation AND operation #AndOperation
    | operation OR operation #OrOperation
    | clause #SimpleClause
    ;

clause: fileType ('.' symbol)? 'WHERE' '(' keyValuePair (',' keyValuePair)* ')'*;
fileType: 'JAVA' | 'java' | 'POM' | 'pom' | 'TEXT' | 'text' | 'PROPERTY' | 'property' | 'YAML' | 'yaml' | 'JSON' | 'json';
symbol: ID;
keyValuePair: key '=' value;
key: QUOTED_STRING | ID;
value: QUOTED_STRING | ID;
logicalOp: AND | OR;

// LEXER vocabulary of the language
FIND:  'FIND';
WHERE: 'WHERE';
AND:   'AND';
OR:    'OR';

ID:            [a-zA-Z][a-zA-Z0-9-]*; // Identifier, allows dots for package names
QUOTED_STRING: '\'' ( ~('\''|'\\') | '\\' . )* '\''; // Single-quoted string
EQUALS:        '=';
DOT:           '.';
COMMA:         ',';
LPAREN:        '(';
RPAREN:        ')';

WS:            [ \t\r\n]+ -> skip; // Skip whitespace