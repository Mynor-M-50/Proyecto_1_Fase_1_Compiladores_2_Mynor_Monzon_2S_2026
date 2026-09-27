parser grammar YLangParser;

// ============================================================
// Y? (.y): parser. Los bloques van por indentacion; los tokens
// NEWLINE, INDENT y DEDENT los arma YLangLexerBase.
// Un .y tiene %estructuras (opcional) y %funciones, y cada funcion
// empieza con "definir". Lo de "mientras (...) hacer:" y los ':' de
// cada bloque salen del enunciado, falta probarlos con mas ejemplos.
// ============================================================

options {
    tokenVocab = YLangLexer;
}

// archivo .y: estructuras (opcional) y funciones
programa
    : NEWLINE* seccionEstructuras? seccionFunciones EOF
    ;

// Las secciones NO indentan su contenido: en el utils.y real, "definir"
// va en la columna 0 justo debajo de "%funciones".
seccionEstructuras
    : SECCION_ESTRUCTURAS NEWLINE estructura+
    ;

// %funciones y la lista de funciones
seccionFunciones
    : SECCION_FUNCIONES NEWLINE funcionDef+
    ;

// estructura Nombre: con sus campos indentados
estructura
    : ESTRUCTURA ID COLON NEWLINE INDENT campoEstructura+ DEDENT
    ;

// tipo nombre, o tipo nombre[N] para un arreglo fijo
campoEstructura
    : tipo ID (LBRACKET ENTERO_LITERAL RBRACKET)? NEWLINE
    ;

// definir nombre(params) -> tipo: (el -> tipo es opcional)
funcionDef
    : DEFINIR ID LPAREN parametros? RPAREN (ARROW tipo)? COLON NEWLINE INDENT sentencia+ DEDENT
    ;

// lista de parametros separados por coma
parametros
    : parametro (COMMA parametro)*
    ;

// tipo nombre, con [] si es arreglo
parametro
    : tipo ID (LBRACKET RBRACKET)?
    ;

// tipo primitivo o nombre de estructura, con [] opcionales
tipo
    : tipoPrimitivo (LBRACKET RBRACKET)*
    | ID (LBRACKET RBRACKET)*        // tipo estructura, posiblemente anidada
    ;

// entero, flotante, caracter, cadena, bool
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

// tipo nombre = valor, o tipo nombre[N]
declaracionVariable
    : tipo (LBRACKET RBRACKET)* ID (LBRACKET ENTERO_LITERAL RBRACKET)* (ASSIGN expresion)? NEWLINE
    ;

// asignacion o expresion suelta (llamada, imprimir, i++)
sentenciaExpresion
    : expresion (ASSIGN expresion)? NEWLINE
    ;

// si (c) entonces / sino (c) entonces / contrario
sentenciaSi
    : SI LPAREN expresion RPAREN ENTONCES bloqueIndentado
      (SINO LPAREN expresion RPAREN ENTONCES bloqueIndentado)*
      (CONTRARIO bloqueIndentado)?
    ;

// salto de linea y sentencias con un nivel mas de indentacion
bloqueIndentado
    : NEWLINE INDENT sentencia+ DEDENT
    ;

// elegir (valor): con sus casos
sentenciaElegir
    : ELEGIR LPAREN expresion RPAREN COLON NEWLINE INDENT casoElegir* casoSiempre? DEDENT
    ;

// caso valor: bloque
casoElegir
    : CASO literalCaso COLON bloqueIndentado
    ;

// siempre: bloque (el caso por defecto)
casoSiempre
    : SIEMPRE COLON bloqueIndentado
    ;

// valores permitidos en un caso
literalCaso
    : ENTERO_LITERAL | CADENA_LITERAL | CARACTER_LITERAL
    ;

// para (init; condicion; update):
sentenciaPara
    : PARA LPAREN declaracionParaInit SEMI expresion SEMI expresion RPAREN COLON bloqueIndentado
    ;

// variable del para
declaracionParaInit
    : tipo ID (ASSIGN expresion)?
    ;

// mientras (condicion) hacer:
sentenciaMientras
    : MIENTRAS LPAREN expresion RPAREN HACER COLON bloqueIndentado
    ;

// hacer: bloque mientras (condicion)
sentenciaHacerMientras
    : HACER COLON bloqueIndentado MIENTRAS LPAREN expresion RPAREN
    ;

// retornar con valor opcional
sentenciaRetornar
    : RETORNAR expresion? NEWLINE
    ;

// romper (break)
sentenciaRomper
    : ROMPER NEWLINE
    ;

// continuar (continue)
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

// argumentos de una llamada
argumentos
    : expresion (COMMA expresion)*
    ;
