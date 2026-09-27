grammar Zetariano;

// ============================================================
// Zetariano (.z): lenguaje orientado a objetos parecido a Java.
// Cada archivo trae una sola clase y se llama igual que ella.
// No hay herencia ni polimorfismo. Algunas reglas aceptan de mas
// (por ejemplo el lado izquierdo de una asignacion) y es el
// semantico el que da el error exacto.
// ============================================================

// ---------- PARSER ----------

programa
    : clase EOF
    ;

// public class Nombre { ... }
clase
    : PUBLIC CLASS ID LBRACE miembro* RBRACE
    ;

// lo que puede ir dentro de la clase
miembro
    : campo
    | constructor
    | metodo
    ;

// atributo, con valor inicial opcional
campo
    : modificador? tipo (LBRACKET RBRACKET)* ID (ASSIGN expresion)? SEMI
    ;

// public / private (no se validan, no hay encapsulamiento)
modificador
    : PUBLIC | PRIVATE
    ;

// public Nombre(params) { ... }
constructor
    : PUBLIC ID LPAREN parametros? RPAREN bloque
    ;

// public tipo nombre(params) { ... }
metodo
    : PUBLIC (tipo | VOID) (LBRACKET RBRACKET)* ID LPAREN parametros? RPAREN bloque
    ;

// lista de parametros separados por coma
parametros
    : parametro (COMMA parametro)*
    ;

// un parametro: tipo nombre
parametro
    : tipo (LBRACKET RBRACKET)* ID
    ;

// tipo primitivo o nombre de una clase
tipo
    : tipoPrimitivo
    | ID          // tipo objeto: el nombre de otra clase definida en otro archivo .z
    ;

// int, double, char, boolean, String
tipoPrimitivo
    : KW_INT
    | KW_DOUBLE
    | KW_CHAR
    | KW_BOOLEAN
    | KW_STRING
    ;

// { sentencias }
bloque
    : LBRACE sentencia* RBRACE
    ;

// todo lo que puede ir dentro de un bloque
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

// tipo nombre = valor;
declaracionVariable
    : tipo (LBRACKET RBRACKET)* ID (LBRACKET RBRACKET)* (ASSIGN expresion)? SEMI
    ;

// Cubre: llamadas (println(...), obj.metodo()), incremento/decremento
// sueltos (a++;) y asignaciones (x = ..., x += ..., etc). El lado
// izquierdo se valida en semantico, no aqui.
sentenciaExpresion
    : expresion (operadorAsignacion expresion)? SEMI
    ;

// = += -= *=
operadorAsignacion
    : ASSIGN | PLUS_ASSIGN | MINUS_ASSIGN | STAR_ASSIGN
    ;

// if / else if / else
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

// switch con sus case y default
sentenciaSwitch
    : SWITCH LPAREN expresion RPAREN LBRACE casoSwitch* casoDefault? RBRACE
    ;

// case valor: sentencias
casoSwitch
    : CASE literalCaso COLON sentencia*
    ;

// default: sentencias
casoDefault
    : DEFAULT COLON sentencia*
    ;

// valores permitidos en un case
literalCaso
    : INT_LITERAL
    | STRING_LITERAL
    | CHAR_LITERAL
    ;

// for (init; condicion; update)
sentenciaFor
    : FOR LPAREN forInit? SEMI expresion? SEMI forUpdate? RPAREN sentenciaOBloque
    ;

// declaracion o expresiones al inicio del for
forInit
    : declaracionVariableSinPuntoYComa
    | expresionLista
    ;

// misma forma que declaracionVariable pero sin el ';' final (va dentro del for)
declaracionVariableSinPuntoYComa
    : tipo (LBRACKET RBRACKET)* ID (ASSIGN expresion)?
    ;

// lo que se ejecuta al final de cada vuelta
forUpdate
    : expresionLista
    ;

// expresiones separadas por coma
expresionLista
    : expresion (COMMA expresion)*
    ;

// while (condicion)
sentenciaWhile
    : WHILE LPAREN expresion RPAREN sentenciaOBloque
    ;

// do { } while (condicion);
sentenciaDoWhile
    : DO bloque WHILE LPAREN expresion RPAREN SEMI
    ;

// return con valor opcional
sentenciaReturn
    : RETURN expresion? SEMI
    ;

// break;
sentenciaBreak
    : BREAK SEMI
    ;

// continue;
sentenciaContinue
    : CONTINUE SEMI
    ;

// ---------- EXPRESIONES (precedencia por orden de alternativa) ----------
expresion
    : PRINTLN LPAREN expresion? RPAREN                         # expLlamadaPrintln
    | PRINT LPAREN expresion? RPAREN                            # expLlamadaPrint
    | READLN LPAREN RPAREN                                      # expLlamadaReadln
    | ID LPAREN argumentos? RPAREN                              # expLlamadaLocal
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

// argumentos de una llamada
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

// operadores de dos caracteres (van antes que los de uno)
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

// operadores y signos de un caracter
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

// literales: numeros, caracteres y cadenas
DOUBLE_LITERAL: [0-9]+ '.' [0-9]+ ;
INT_LITERAL   : [0-9]+ ;
CHAR_LITERAL  : '\'' ( ~['\\\r\n] | '\\' . ) '\'' ;
STRING_LITERAL: '"' ( ~["\\\r\n] | '\\' . )* '"' ;

// identificadores
ID: [a-zA-Z_][a-zA-Z_0-9]* ;

// comentarios: al canal oculto para que el editor los pueda colorear
LINE_COMMENT : '//' ~[\r\n]* -> channel(HIDDEN) ;
BLOCK_COMMENT: '/*' .*? '*/' -> channel(HIDDEN) ;
WS           : [ \t\r\n]+ -> skip ;
