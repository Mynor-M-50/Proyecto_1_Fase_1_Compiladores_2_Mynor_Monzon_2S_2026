lexer grammar YLangLexer;

// ============================================================
// Y? (.y): lexer. Como los bloques se marcan con indentacion,
// NEWLINE, INDENT y DEDENT no salen de ninguna regla de aca: los
// arma YLangLexerBase a partir de NEWLINE_RAW.
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

// tipos primitivos
KW_ENTERO  : 'entero' ;
KW_FLOTANTE: 'flotante' ;
KW_CARACTER: 'caracter' ;
KW_CADENA  : 'cadena' ;
KW_BOOL    : 'bool' ;

// flecha del tipo de retorno: definir f() -> entero:
ARROW: '->' ;

// operadores de dos caracteres (van antes que los de uno)
PLUSPLUS  : '++' ;
MINUSMINUS: '--' ;
EQ  : '==' ;
NEQ : '!=' ;
AND : '&&' ;
OR  : '||' ;

// operadores y signos de un caracter
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

// literales: numeros, caracteres y cadenas
FLOTANTE_LITERAL: [0-9]+ '.' [0-9]+ ;
ENTERO_LITERAL  : [0-9]+ ;
CARACTER_LITERAL: '\'' ( ~['\\\r\n] | '\\' . ) '\'' ;
CADENA_LITERAL  : '"' ( ~["\\\r\n] | '\\' . )* '"' ;

// identificadores
ID: [a-zA-Z_][a-zA-Z_0-9]* ;

// comentarios: al canal oculto para que el editor los pueda colorear
LINE_COMMENT : '//' ~[\r\n]* -> channel(HIDDEN) ;
BLOCK_COMMENT: '/*' .*? '*/' -> channel(HIDDEN) ;

// Espacios que NO estan al inicio de linea: se ignoran normal.
SPACES: [ \t]+ -> skip ;

// Salto(s) de linea + la indentacion de la siguiente linea, todo junto.
// NO se marca "-> skip": YLangLexerBase.nextToken() lo intercepta y
// decide que hacer (emitir NEWLINE, NEWLINE+INDENT, NEWLINE+DEDENT*,
// o descartarlo si la siguiente linea esta en blanco / es comentario).
NEWLINE_RAW: ('\r'? '\n' [ \t]*)+ ;
