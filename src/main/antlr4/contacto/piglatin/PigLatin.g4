grammar PigLatin;

// ============================================================
// Gramatica de Pig Latin (Proyecto 1 - Compiladores 2). Extension: .pig
//
// Adaptada de CodexLatinus.g4 (Practica 1) con los cambios que pide el
// proyecto nuevo:
//   - YA NO se definen estructuras (STRUCTURA) ni funciones (MUNERA/
//     ACTIO/RATIO) dentro del .pig: se importan de archivos .y/.z.
//   - Se agrega una seccion de "import" al inicio del archivo.
//   - Se agrega "novus" para instanciar objetos de una clase importada,
//     y llamadas a metodo sobre un objetivo (obj.metodo(args)).
//
// Confirmado contra main.pig real (el que compartio la auxiliar):
//   - El cierre "FINIS" (mayuscula) de la seccion MAIOR es OPCIONAL en
//     la practica (el ejemplo real no lo trae), aunque el documento
//     original del proyecto si lo muestra. Lo dejo opcional para
//     aceptar ambos casos.
//   - El ';' (PYC) es opcional practicamente en todas partes, igual
//     que en CodexLatinus.
// ============================================================

// ---------- PARSER ----------

programa
    : seccionImports? seccionVariables? seccionMaior EOF
    ;

seccionImports
    : (IMPORT rutaImport)*
    ;

rutaImport
    : ID (PUNTO ID)*
    ;

seccionVariables
    : VARIABILES MAYOR declaracion*
    ;

seccionMaior
    : MAIOR MAYOR instruccion* FIN_PROGRAMA? PYC?
    ;

// -----
// TIPOS
// -----

tipo
    : NUMERUS       # TipoNumerus
    | DECIMALIS     # TipoDecimalis
    | TEXTUM        # TipoTextum
    | LITTERA       # TipoLittera
    | BOOL          # TipoBool
    | ID            # TipoEstructura
    ;

valorBooleano
    : VERUM
    | FALSUS
    ;

// -------------
// DECLARACIONES
// -------------

declaracion
    : declaracionVariable
    | declaracionArreglo
    ;

// El orden de las alternativas importa:
//  1 objeto (novus)   2 estructura   3 booleana   4 con valor   5 sin valor
declaracionVariable
    : ESTO ID DOS_PUNTOS NOVUS ID PAR_A listaArgumentos? PAR_C PYC?  # DeclObjeto
    | ESTO ID DOS_PUNTOS ID literalEstructura PYC?                  # DeclEstructura
    | ESTO ID DOS_PUNTOS valorBooleano expresion? PYC?              # DeclBooleana
    | ESTO ID DOS_PUNTOS tipo expresion PYC?                        # DeclConValor
    | ESTO ID DOS_PUNTOS tipo PYC?                                  # DeclSinValor
    ;

// series id[dim] : tipo {valores};
declaracionArreglo
    : SERIES ID dimension? DOS_PUNTOS tipo listaValores? PYC?  # ArregloTipado
    | SERIES ID dimension? DOS_PUNTOS listaValores PYC?        # ArregloInferido
    ;

dimension
    : COR_A expresion COR_C
    ;

listaValores
    : LLAVE_A (valorLista (COMA valorLista)*)? COMA? LLAVE_C
    ;

// Un elemento de la lista puede ser una instancia de estructura, para
// permitir  series personas[2] : Persona {{...}, {...}};
valorLista
    : literalEstructura
    | expresion
    ;

literalEstructura
    : LLAVE_A (asignacionAtributo (separadorCampo asignacionAtributo)*)? separadorCampo? LLAVE_C
    ;

asignacionAtributo
    : ID DOS_PUNTOS valorAtributo
    ;

valorAtributo
    : literalEstructura
    | listaValores
    | dimensionPrimitiva
    | expresion
    ;

// Fija la dimension de un arreglo de tipo primitivo dentro de una
// instancia de estructura (viene definida en el .y importado)
dimensionPrimitiva
    : (NUMERUS | DECIMALIS | TEXTUM | LITTERA | BOOL) COR_A expresion COR_C
    ;

separadorCampo
    : PYC
    | COMA
    ;

// ----------------------------------------------------
// LLAMADAS (a funciones libres importadas de .y, o a metodos de un
// objeto importado de .z -- via objetivo, ver mas abajo)
// ----------------------------------------------------

llamadaFuncion
    : ID PAR_A listaArgumentos? PAR_C
    ;

listaArgumentos
    : expresion (COMA expresion)*
    ;

// -------------
// INSTRUCCIONES
// -------------

instruccion
    : asignacion
    | incremento
    | condicional
    | cicloDum
    | cicloFacere
    | cicloPer
    | perge
    | interrumpe
    | reddere
    | imprimir
    | leer
    | llamadaInstruccion
    | declaracionVariable
    | declaracionArreglo
    ;

bloque
    : LLAVE_A instruccion* LLAVE_C
    ;

// Cubre: id, id[expr], id.attr, id.attr[expr].attr, id.metodo(args)
objetivo
    : ID sufijoAcceso*
    ;

sufijoAcceso
    : PUNTO ID PAR_A listaArgumentos? PAR_C   # SufijoMetodo
    | PUNTO ID                                 # SufijoAtributo
    | COR_A expresion COR_C                    # SufijoIndice
    ;

asignacion
    : objetivo IGUAL literalEstructura PYC?   # AsignacionEstructura
    | objetivo IGUAL listaValores PYC?        # AsignacionLista
    | objetivo IGUAL expresion PYC?           # AsignacionSimple
    ;

// ++ y -- se aplican en cualquier ambito
incremento
    : objetivo (MASMAS | MENOSMENOS) PYC?
    ;

llamadaInstruccion
    : llamadaFuncion PYC?     # LlamadaFuncionInstruccion
    | objetivo PYC?           # LlamadaMetodoInstruccion
    ;

// -------------
// CONDICIONALES
// si (...) {} aliter (...) {} aliter {} finis;
// -------------

condicional
    : SI PAR_A expresion PAR_C bloque
      ramaAliterSi*
      ramaAliter?
      FINIS PYC?
    ;

ramaAliterSi
    : ALITER PAR_A expresion PAR_C bloque
    ;

ramaAliter
    : ALITER bloque
    ;

// -----
// CICLOS
// -----

cicloDum
    : DUM PAR_A expresion PAR_C bloque FINIS PYC?
    ;

cicloFacere
    : FACERE bloque DUM PAR_A expresion PAR_C PYC?
    ;

cicloPer
    : PER PAR_A inicializacionPer PYC expresion PYC actualizacionPer PAR_C
      bloque (FINIS PYC?)?
    ;

inicializacionPer
    : ESTO ID DOS_PUNTOS tipo expresion   # PerDeclara
    | objetivo IGUAL expresion            # PerAsigna
    ;

actualizacionPer
    : objetivo (MASMAS | MENOSMENOS)      # PerIncremento
    | objetivo IGUAL expresion            # PerAsignacion
    ;

perge
    : PERGE PYC?
    ;

interrumpe
    : INTERRUMPE PYC?
    ;

reddere
    : REDDERE expresion? PYC?
    ;

// --------------------------------
// FUNCIONES ESPECIALES DEL SISTEMA
// >> imprime   |   << lee
// --------------------------------

// El '>>' entre valores es OBLIGATORIO: como el ';' tambien es opcional,
// con '>>'? la linea siguiente ("opcion <<") se pegaba a la impresion
// como un valor mas y la lectura quedaba descartada.
imprimir
    : MAYORMAYOR expresion (MAYORMAYOR expresion)* PYC?
    ;

leer
    : objetivo MENORMENOR PYC?   # LeerEnVariable
    | MENORMENOR PYC?            # LeerDescartado
    ;

// -----------
// EXPRESIONES
// -----------

expresion
    : PAR_A expresion PAR_C                                          # ExprAgrupada
    | NOVUS ID PAR_A listaArgumentos? PAR_C                          # ExprNuevoObjeto
    | (MENOS | NON) expresion                                        # ExprUnaria
    | expresion (POR | DIV) expresion                                # ExprMulDiv
    | expresion (MAS | MENOS) expresion                              # ExprSumaResta
    | expresion (MENOR | MAYOR | MENORIGUAL | MAYORIGUAL) expresion  # ExprRelacional
    | expresion (IGUALIGUAL | DIFERENTE) expresion                   # ExprIgualdad
    | expresion AND expresion                                        # ExprAnd
    | expresion OR expresion                                         # ExprOr
    | llamadaFuncion                                                 # ExprLlamada
    | objetivo                                                       # ExprAcceso
    | literal                                                        # ExprLiteral
    ;

literal
    : ENTERO    # LitEntero
    | DECIMAL   # LitDecimal
    | CADENA    # LitCadena
    | CARACTER  # LitCaracter
    | VERUM     # LitVerum
    | FALSUS    # LitFalsus
    ;

// ----------------
// REGLAS DEL LEXER (identicas a CodexLatinus + IMPORT + NOVUS)
// El lenguaje es case sensitive.
// ----------------

VARIABILES : 'VARIABILES';
MAIOR      : 'MAIOR';

FIN_PROGRAMA : 'FINIS';

IMPORT    : 'import';
ESTO      : 'esto';
SERIES    : 'series';
NOVUS     : 'novus';
FINIS     : 'finis';

NUMERUS   : 'numerus';
DECIMALIS : 'decimalis';
TEXTUM    : 'textum';
LITTERA   : 'littera';
BOOL      : 'bool';

VERUM  : 'verum';
FALSUS : 'falsus';

SI         : 'si';
ALITER     : 'aliter';
DUM        : 'dum';
FACERE     : 'facere';
PER        : 'per';
PERGE      : 'perge';
INTERRUMPE : 'interrumpe';

REDDERE : 'reddere';

NON : 'non';

MASMAS     : '++';
MENOSMENOS : '--';
MAYORMAYOR : '>>';
MENORMENOR : '<<';
IGUALIGUAL : '==';
DIFERENTE  : '!=';
MENORIGUAL : '<=';
MAYORIGUAL : '>=';
AND        : '&&';
OR         : '||';

MAS   : '+';
MENOS : '-';
POR   : '*';
DIV   : '/';
IGUAL : '=';
MENOR : '<';
MAYOR : '>';

PAR_A      : '(';
PAR_C      : ')';
LLAVE_A    : '{';
LLAVE_C    : '}';
COR_A      : '[';
COR_C      : ']';
PYC        : ';';
COMA       : ',';
PUNTO      : '.';
DOS_PUNTOS : ':';

DECIMAL : [0-9]+ '.' [0-9]+ ;
ENTERO  : [0-9]+ ;

CADENA
    : '"' ( '\\' . | ~["\\\r\n] )* '"'
    | '\u201C' ( ~[\u201C\u201D\r\n] )* '\u201D'
    ;

CARACTER
    : '\'' ( '\\' . | ~['\\\r\n] ) '\''
    ;

ID : [a-zA-Z_] [a-zA-Z_0-9]* ;

COMENTARIO_BLOQUE : '##' .*? '##' -> skip ;
COMENTARIO_LINEA  : '//' ~[\r\n]* -> skip ;
ESPACIOS          : [ \t\r\n]+ -> skip ;
