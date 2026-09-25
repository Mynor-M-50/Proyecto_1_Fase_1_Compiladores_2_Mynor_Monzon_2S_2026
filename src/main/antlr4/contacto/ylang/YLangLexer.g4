lexer grammar YLangLexer;

// ============================================================
// Lexer del lenguaje Y? (Proyecto 1 - Compiladores 2). Extension: .y
// Los bloques se delimitan por INDENTACION (como Python), no por
// llaves. INDENT/DEDENT/NEWLINE son tokens SINTETICOS que no genera
// ninguna regla de este lexer directamente: los arma YLangLexerBase
// interceptando NEWLINE_RAW en nextToken(). Ver ese archivo para el
// detalle del algoritmo (pila de niveles de indentacion).
// ============================================================

options {
    superClass = YLangLexerBase;
}

tokens { INDENT, DEDENT, NEWLINE }

// ---- Secciones y palabras clave ----
SECCION_ESTRUCTURAS: '%estructuras' ;
SECCION_FUNCIONES  : '%funciones' ;
ESTRUCTURA: 'estructura' ;
DEFINIR   : 'definir' ;
SI        : 'si' ;
ENTONCES  : 'entonces' ;
SINO      : 'sino' ;
CONTRARIO : 'contrario' ;
ELEGIR    : 'elegir' ;
CASO      : 'caso' ;
SIEMPRE   : 'siempre' ;
ROMPER    : 'romper' ;
PARA      : 'para' ;
MIENTRAS  : 'mientras' ;
HACER     : 'hacer' ;
CONTINUAR : 'continuar' ;
RETORNAR  : 'retornar' ;
IMPRIMIR  : 'imprimir' ;
LEER      : 'leer' ;
VERDADERO : 'verdadero' ;
FALSO     : 'falso' ;

KW_ENTERO  : 'entero' ;
KW_FLOTANTE: 'flotante' ;
KW_CARACTER: 'caracter' ;
KW_CADENA  : 'cadena' ;
KW_BOOL    : 'bool' ;

ARROW: '->' ;

PLUSPLUS  : '++' ;
MINUSMINUS: '--' ;
EQ  : '==' ;
NEQ : '!=' ;
AND : '&&' ;
OR  : '||' ;

PLUS  : '+' ;
MINUS : '-' ;
STAR  : '*' ;
SLASH : '/' ;
ASSIGN: '=' ;
LT    : '<' ;
GT    : '>' ;
NOT   : '!' ;
LPAREN: '(' ;
RPAREN: ')' ;
LBRACKET: '[' ;
RBRACKET: ']' ;
LBRACE: '{' ;   // solo para literales de arreglo/estructura: {10, 20, 30}
RBRACE: '}' ;
COLON : ':' ;
COMMA : ',' ;
DOT   : '.' ;
SEMI  : ';' ;   // solo se usa dentro del encabezado de "para(...)"

FLOTANTE_LITERAL: [0-9]+ '.' [0-9]+ ;
ENTERO_LITERAL  : [0-9]+ ;
CARACTER_LITERAL: '\'' ( ~['\\\r\n] | '\\' . ) '\'' ;
CADENA_LITERAL  : '"' ( ~["\\\r\n] | '\\' . )* '"' ;

ID: [a-zA-Z_][a-zA-Z_0-9]* ;

LINE_COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT: '/*' .*? '*/' -> skip ;

// Espacios que NO estan al inicio de linea: se ignoran normal.
SPACES: [ \t]+ -> skip ;

// Salto(s) de linea + la indentacion de la siguiente linea, todo junto.
// NO se marca "-> skip": YLangLexerBase.nextToken() lo intercepta y
// decide que hacer (emitir NEWLINE, NEWLINE+INDENT, NEWLINE+DEDENT*,
// o descartarlo si la siguiente linea esta en blanco / es comentario).
NEWLINE_RAW: ('\r'? '\n' [ \t]*)+ ;