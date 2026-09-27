grammar PigLatin;

// ============================================================
// Pig Latin (.pig): el programa principal. Sale de la gramatica de
// CodexLatinus (practica 1), pero aca ya no se definen estructuras
// ni funciones: se importan de archivos .y y .z. Se agrego import,
// novus para crear objetos y llamadas obj.metodo(...).
// El FINIS del final y los ';' son opcionales.
// ============================================================

// ---------- PARSER ----------

programa
    : seccionImports? seccionVariables? seccionMaior EOF
    ;

// import Archivo.z / import carpeta.archivo.y
seccionImports
    : (IMPORT rutaImport)*
    ;

// ruta del import separada por puntos
rutaImport
    : ID (PUNTO ID)*
    ;

// VARIABILES> y las declaraciones globales
seccionVariables
    : VARIABILES MAYOR declaracion*
    ;

// MAIOR> y las instrucciones del programa
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

// verum / falsus
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

// [tamanio] de un arreglo
dimension
    : COR_A expresion COR_C
    ;

// {v1, v2, ...}
listaValores
    : LLAVE_A (valorLista (COMA valorLista)*)? COMA? LLAVE_C
    ;

// Un elemento de la lista puede ser una instancia de estructura, para
// permitir  series personas[2] : Persona {{...}, {...}};
valorLista
    : literalEstructura
    | expresion
    ;

// {campo: valor; campo: valor}
literalEstructura
    : LLAVE_A (asignacionAtributo (separadorCampo asignacionAtributo)*)? separadorCampo? LLAVE_C
    ;

// campo: valor
asignacionAtributo
    : ID DOS_PUNTOS valorAtributo
    ;

// lo que puede ir como valor de un campo
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

// los campos se separan con ';' o ','
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

// argumentos separados por coma
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

// { instrucciones }
bloque
    : LLAVE_A instruccion* LLAVE_C
    ;

// Cubre: id, id[expr], id.attr, id.attr[expr].attr, id.metodo(args)
objetivo
    : ID sufijoAcceso*
    ;

// .metodo(args), .campo o [indice]
sufijoAcceso
    : PUNTO ID PAR_A listaArgumentos? PAR_C   # SufijoMetodo
    | PUNTO ID                                 # SufijoAtributo
    | COR_A expresion COR_C                    # SufijoIndice
    ;

// objetivo = valor (tambien estructura o lista)
asignacion
    : objetivo IGUAL literalEstructura PYC?   # AsignacionEstructura
    | objetivo IGUAL listaValores PYC?        # AsignacionLista
    | objetivo IGUAL expresion PYC?           # AsignacionSimple
    ;

// ++ y -- se aplican en cualquier ambito
incremento
    : objetivo (MASMAS | MENOSMENOS) PYC?
    ;

// llamada suelta a una funcion o a un metodo
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

// aliter (condicion) { } = else if
ramaAliterSi
    : ALITER PAR_A expresion PAR_C bloque
    ;

// aliter { } = else
ramaAliter
    : ALITER bloque
    ;

// -----
// CICLOS
// -----

cicloDum
    : DUM PAR_A expresion PAR_C bloque FINIS PYC?
    ;

// facere { } dum (condicion) = do-while
cicloFacere
    : FACERE bloque DUM PAR_A expresion PAR_C PYC?
    ;

// per (init; condicion; actualizacion) { } = for
cicloPer
    : PER PAR_A inicializacionPer PYC expresion PYC actualizacionPer PAR_C
      bloque (FINIS PYC?)?
    ;

// variable nueva o asignacion al inicio del per
inicializacionPer
    : ESTO ID DOS_PUNTOS tipo expresion   # PerDeclara
    | objetivo IGUAL expresion            # PerAsigna
    ;

// i++ / i-- o una asignacion al final de cada vuelta
actualizacionPer
    : objetivo (MASMAS | MENOSMENOS)      # PerIncremento
    | objetivo IGUAL expresion            # PerAsignacion
    ;

// perge = continue
perge
    : PERGE PYC?
    ;

// interrumpe = break
interrumpe
    : INTERRUMPE PYC?
    ;

// reddere = return
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

// x << lee en una variable; << solo lee y descarta
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

// numeros, cadenas, caracteres y booleanos
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

COMENTARIO_BLOQUE : '##' .*? '##' -> channel(HIDDEN) ;
COMENTARIO_LINEA  : '//' ~[\r\n]* -> channel(HIDDEN) ;
ESPACIOS          : [ \t\r\n]+ -> skip ;
