parser grammar YLangParser;

// ============================================================
// Parser del lenguaje Y? (Proyecto 1 - Compiladores 2)
//
// Confirmado contra un utils.y real que compartio la auxiliar (ver
// notes/NOTES.md): las funciones SI llevan la palabra clave "definir"
// antes del nombre, y "%funciones" / "%estructuras" NO llevan ':'
// despues (van directo a NEWLINE). Ya corregido abajo.
//
// SUPUESTOS que siguen sin confirmar:
//   1. Cada encabezado de bloque que SI abre con ':' (estructura,
//      funcion, si/sino/contrario, elegir, para, mientras, hacer) lo
//      hace de forma consistente - esto si esta confirmado para
//      funciones (definir ...():), falta confirmar para el resto.
//   2. mientras usa "mientras(cond) hacer:": doble palabra clave antes
//      del bloque (asi aparece en el documento fuente, sin ejemplo real
//      todavia que lo confirme).
//   3. Igual que en Codex Latinus: la gramatica es permisiva a proposito
//      en el lado izquierdo de una asignacion (se acepta cualquier
//      "expresion"), y es el analizador semantico el que valida que sea
//      un lvalue real.
// ============================================================

options {
    tokenVocab = YLangLexer;
}

programa
    : NEWLINE* seccionEstructuras? seccionFunciones EOF
    ;

seccionEstructuras
    : SECCION_ESTRUCTURAS NEWLINE INDENT estructura+ DEDENT
    ;

seccionFunciones
    : SECCION_FUNCIONES NEWLINE INDENT funcionDef+ DEDENT
    ;

estructura
    : ESTRUCTURA ID COLON NEWLINE INDENT campoEstructura+ DEDENT
    ;

campoEstructura
    : tipo ID (LBRACKET ENTERO_LITERAL RBRACKET)? NEWLINE
    ;

funcionDef
    : DEFINIR ID LPAREN parametros? RPAREN (ARROW tipo)? COLON NEWLINE INDENT sentencia+ DEDENT
    ;

parametros
    : parametro (COMMA parametro)*
    ;

parametro
    : tipo ID (LBRACKET RBRACKET)?
    ;

tipo
    : tipoPrimitivo (LBRACKET RBRACKET)*
    | ID (LBRACKET RBRACKET)*        // tipo estructura, posiblemente anidada
    ;

tipoPrimitivo
    : KW_ENTERO | KW_FLOTANTE | KW_CARACTER | KW_CADENA | KW_BOOL
    ;

// ---------- sentencias ----------
sentencia
    : declaracionVariable
    | estructura                       // se permiten estructuras locales dentro de funciones
    | sentenciaExpresion
    | sentenciaSi
    | sentenciaElegir
    | sentenciaPara
    | sentenciaMientras
    | sentenciaHacerMientras
    | sentenciaRetornar
    | sentenciaRomper
    | sentenciaContinuar
    ;

declaracionVariable
    : tipo (LBRACKET RBRACKET)* ID (LBRACKET ENTERO_LITERAL RBRACKET)* (ASSIGN expresion)? NEWLINE
    ;

sentenciaExpresion
    : expresion (ASSIGN expresion)? NEWLINE
    ;

sentenciaSi
    : SI LPAREN expresion RPAREN ENTONCES bloqueIndentado
      (SINO LPAREN expresion RPAREN ENTONCES bloqueIndentado)*
      (CONTRARIO bloqueIndentado)?
    ;

bloqueIndentado
    : NEWLINE INDENT sentencia+ DEDENT
    ;

sentenciaElegir
    : ELEGIR LPAREN expresion RPAREN COLON NEWLINE INDENT casoElegir* casoSiempre? DEDENT
    ;

casoElegir
    : CASO literalCaso COLON bloqueIndentado
    ;

casoSiempre
    : SIEMPRE COLON bloqueIndentado
    ;

literalCaso
    : ENTERO_LITERAL | CADENA_LITERAL | CARACTER_LITERAL
    ;

sentenciaPara
    : PARA LPAREN declaracionParaInit SEMI expresion SEMI expresion RPAREN COLON bloqueIndentado
    ;

declaracionParaInit
    : tipo ID (ASSIGN expresion)?
    ;

sentenciaMientras
    : MIENTRAS LPAREN expresion RPAREN HACER COLON bloqueIndentado
    ;

sentenciaHacerMientras
    : HACER COLON bloqueIndentado MIENTRAS LPAREN expresion RPAREN
    ;

sentenciaRetornar
    : RETORNAR expresion? NEWLINE
    ;

sentenciaRomper
    : ROMPER NEWLINE
    ;

sentenciaContinuar
    : CONTINUAR NEWLINE
    ;

// ---------- expresiones (precedencia por orden de alternativa) ----------
expresion
    : IMPRIMIR LPAREN expresion? RPAREN                # expImprimir
    | LEER LPAREN RPAREN                                # expLeer
    | ID LPAREN argumentos? RPAREN                      # expLlamadaFuncion
    | expresion LBRACKET expresion RBRACKET             # expIndice
    | expresion DOT ID                                  # expAcceso
    | LPAREN expresion RPAREN                            # expParentesis
    | (PLUSPLUS | MINUSMINUS) expresion                 # expIncDecPrefijo
    | expresion (PLUSPLUS | MINUSMINUS)                 # expIncDecSufijo
    | (NOT | MINUS) expresion                            # expUnario
    | expresion (STAR | SLASH) expresion                 # expMultiplicativa
    | expresion (PLUS | MINUS) expresion                 # expAditiva
    | expresion (LT | GT) expresion                      # expRelacional
    | expresion (EQ | NEQ) expresion                     # expIgualdad
    | expresion AND expresion                             # expAnd
    | expresion OR expresion                              # expOr
    | LBRACE (expresion (COMMA expresion)*)? RBRACE        # expLiteralCompuesto
    | ID                                                   # expId
    | ENTERO_LITERAL                                       # expEntero
    | FLOTANTE_LITERAL                                     # expFlotante
    | CARACTER_LITERAL                                     # expCaracter
    | CADENA_LITERAL                                       # expCadena
    | VERDADERO                                            # expVerdadero
    | FALSO                                                # expFalso
    ;

argumentos
    : expresion (COMMA expresion)*
    ;