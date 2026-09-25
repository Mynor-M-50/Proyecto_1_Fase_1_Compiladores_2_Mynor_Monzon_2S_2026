grammar Zetariano;

// ============================================================
// Gramatica del lenguaje ZETARIANO (Proyecto 1 - Compiladores 2)
// Extension de archivo: .z
// El archivo debe llamarse igual que la clase definida adentro.
// Notas de diseno:
//  - No hay encapsulamiento, herencia ni polimorfismo (fuera de alcance).
//  - Los objetos viven en heap (decision semantica, no de gramatica).
//  - La gramatica es permisiva a proposito en algunos puntos (p.ej. el
//    lado izquierdo de una asignacion es "expresion" en vez de un
//    "lvalue" estricto) para que sea el analizador semantico el que
//    reporte el error puntual, en vez de un error sintactico generico.
// ============================================================

// ---------- PARSER ----------

programa
    : clase EOF
    ;

clase
    : PUBLIC CLASS ID LBRACE miembro* RBRACE
    ;

miembro
    : campo
    | constructor
    | metodo
    ;

campo
    : modificador? tipo (LBRACKET RBRACKET)* ID (ASSIGN expresion)? SEMI
    ;

modificador
    : PUBLIC | PRIVATE
    ;

constructor
    : PUBLIC ID LPAREN parametros? RPAREN bloque
    ;

metodo
    : PUBLIC (tipo | VOID) (LBRACKET RBRACKET)* ID LPAREN parametros? RPAREN bloque
    ;

parametros
    : parametro (COMMA parametro)*
    ;

parametro
    : tipo (LBRACKET RBRACKET)* ID
    ;

tipo
    : tipoPrimitivo
    | ID          // tipo objeto: el nombre de otra clase definida en otro archivo .z
    ;

tipoPrimitivo
    : KW_INT
    | KW_DOUBLE
    | KW_CHAR
    | KW_BOOLEAN
    | KW_STRING
    ;

bloque
    : LBRACE sentencia* RBRACE
    ;

sentencia
    : declaracionVariable
    | sentenciaExpresion
    | sentenciaIf
    | sentenciaSwitch
    | sentenciaFor
    | sentenciaWhile
    | sentenciaDoWhile
    | sentenciaReturn
    | sentenciaBreak
    | sentenciaContinue
    | bloque
    ;

declaracionVariable
    : tipo (LBRACKET RBRACKET)* ID (LBRACKET RBRACKET)* (ASSIGN expresion)? SEMI
    ;

// Cubre: llamadas (println(...), obj.metodo()), incremento/decremento
// sueltos (a++;) y asignaciones (x = ..., x += ..., etc). El lado
// izquierdo se valida en semantico, no aqui.
sentenciaExpresion
    : expresion (operadorAsignacion expresion)? SEMI
    ;

operadorAsignacion
    : ASSIGN | PLUS_ASSIGN | MINUS_ASSIGN | STAR_ASSIGN
    ;

sentenciaIf
    : IF LPAREN expresion RPAREN sentenciaOBloque
      (ELSE IF LPAREN expresion RPAREN sentenciaOBloque)*
      (ELSE sentenciaOBloque)?
    ;

// Permite las llaves opcionales cuando el cuerpo es una sola sentencia
sentenciaOBloque
    : bloque
    | sentencia
    ;

sentenciaSwitch
    : SWITCH LPAREN expresion RPAREN LBRACE casoSwitch* casoDefault? RBRACE
    ;

casoSwitch
    : CASE literalCaso COLON sentencia*
    ;

casoDefault
    : DEFAULT COLON sentencia*
    ;

literalCaso
    : INT_LITERAL
    | STRING_LITERAL
    | CHAR_LITERAL
    ;

sentenciaFor
    : FOR LPAREN forInit? SEMI expresion? SEMI forUpdate? RPAREN sentenciaOBloque
    ;

forInit
    : declaracionVariableSinPuntoYComa
    | expresionLista
    ;

// misma forma que declaracionVariable pero sin el ';' final (va dentro del for)
declaracionVariableSinPuntoYComa
    : tipo (LBRACKET RBRACKET)* ID (ASSIGN expresion)?
    ;

forUpdate
    : expresionLista
    ;

expresionLista
    : expresion (COMMA expresion)*
    ;

sentenciaWhile
    : WHILE LPAREN expresion RPAREN sentenciaOBloque
    ;

sentenciaDoWhile
    : DO bloque WHILE LPAREN expresion RPAREN SEMI
    ;

sentenciaReturn
    : RETURN expresion? SEMI
    ;

sentenciaBreak
    : BREAK SEMI
    ;

sentenciaContinue
    : CONTINUE SEMI
    ;

// ---------- EXPRESIONES (precedencia por orden de alternativa) ----------
expresion
    : PRINTLN LPAREN expresion? RPAREN                         # expLlamadaPrintln
    | PRINT LPAREN expresion? RPAREN                            # expLlamadaPrint
    | READLN LPAREN RPAREN                                      # expLlamadaReadln
    | NEW ID LPAREN argumentos? RPAREN                          # expNuevoObjeto
    | NEW tipoPrimitivo (LBRACKET expresion RBRACKET)+          # expNuevoArreglo
    | expresion LBRACKET expresion RBRACKET                     # expIndice
    | expresion DOT ID LPAREN argumentos? RPAREN                # expLlamadaMetodo
    | expresion DOT ID                                          # expAcceso
    | LPAREN expresion RPAREN                                   # expParentesis
    | (INC | DEC) expresion                                     # expIncDecPrefijo
    | expresion (INC | DEC)                                     # expIncDecSufijo
    | (NOT | MINUS) expresion                                   # expUnario
    | expresion (STAR | SLASH | PERCENT) expresion              # expMultiplicativa
    | expresion (PLUS | MINUS) expresion                        # expAditiva
    | expresion (LT | GT | LE | GE) expresion                   # expRelacional
    | expresion (EQ | NEQ) expresion                            # expIgualdad
    | expresion AND expresion                                   # expAnd
    | expresion OR expresion                                    # expOr
    | expresion QUESTION expresion COLON expresion               # expTernario
    | LBRACE (expresion (COMMA expresion)*)? RBRACE              # expArregloLiteral
    | ID                                                         # expId
    | THIS                                                       # expThis
    | INT_LITERAL                                                # expEntero
    | DOUBLE_LITERAL                                             # expDecimal
    | CHAR_LITERAL                                               # expCaracter
    | STRING_LITERAL                                             # expCadena
    | TRUE                                                       # expVerdadero
    | FALSE                                                      # expFalso
    | NULL                                                       # expNulo
    ;

argumentos
    : expresion (COMMA expresion)*
    ;

// ---------- LEXER ----------
// Palabras reservadas primero para que ganen sobre ID (misma longitud,
// ANTLR4 resuelve el empate a favor de la regla declarada antes).
PUBLIC    : 'public' ;
PRIVATE   : 'private' ;
THIS      : 'this' ;
CLASS     : 'class' ;
KW_INT    : 'int' ;
KW_DOUBLE : 'double' ;
KW_CHAR   : 'char' ;
KW_BOOLEAN: 'boolean' ;
KW_STRING : 'String' ;
VOID      : 'void' ;
NEW       : 'new' ;
NULL      : 'null' ;
TRUE      : 'true' ;
FALSE     : 'false' ;
IF        : 'if' ;
ELSE      : 'else' ;
SWITCH    : 'switch' ;
CASE      : 'case' ;
DEFAULT   : 'default' ;
BREAK     : 'break' ;
FOR       : 'for' ;
WHILE     : 'while' ;
DO        : 'do' ;
CONTINUE  : 'continue' ;
RETURN    : 'return' ;
PRINTLN   : 'println' ;
PRINT     : 'print' ;
READLN    : 'readln' ;

PLUS_ASSIGN : '+=' ;
MINUS_ASSIGN: '-=' ;
STAR_ASSIGN : '*=' ;
INC : '++' ;
DEC : '--' ;
EQ  : '==' ;
NEQ : '!=' ;
LE  : '<=' ;
GE  : '>=' ;
AND : '&&' ;
OR  : '||' ;

PLUS   : '+' ;
MINUS  : '-' ;
STAR   : '*' ;
SLASH  : '/' ;
PERCENT: '%' ;
ASSIGN : '=' ;
LT     : '<' ;
GT     : '>' ;
NOT    : '!' ;
QUESTION: '?' ;
COLON  : ':' ;
LPAREN : '(' ;
RPAREN : ')' ;
LBRACE : '{' ;
RBRACE : '}' ;
LBRACKET: '[' ;
RBRACKET: ']' ;
SEMI   : ';' ;
COMMA  : ',' ;
DOT    : '.' ;

DOUBLE_LITERAL: [0-9]+ '.' [0-9]+ ;
INT_LITERAL   : [0-9]+ ;
CHAR_LITERAL  : '\'' ( ~['\\\r\n] | '\\' . ) '\'' ;
STRING_LITERAL: '"' ( ~["\\\r\n] | '\\' . )* '"' ;

ID: [a-zA-Z_][a-zA-Z_0-9]* ;

LINE_COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT: '/*' .*? '*/' -> skip ;
WS           : [ \t\r\n]+ -> skip ;