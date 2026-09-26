package contacto.comun.codegen;

import contacto.comun.tipos.Tipo;

/**
 * Lo que el C generado necesita y C no trae: concatenar cadenas,
 * convertir numeros a cadena, leer una linea y convertirla, reservar
 * objetos/arreglos en heap y cortar con un mensaje claro si se llama un
 * metodo sobre un objeto nulo (en vez de un "Segmentation fault").
 *
 * Se copia al inicio de cada .c generado. Todas las funciones son
 * "static inline" para que gcc -Wall no avise por las que un programa
 * concreto no use.
 */
public final class RuntimeC {

    private RuntimeC() {
    }

    public static final String CODIGO = String.join("\n",
            "#include <stdio.h>",
            "#include <stdlib.h>",
            "#include <string.h>",
            "#include <stdarg.h>",
            "",
            "/* ===================== runtime del compilador ===================== */",
            "",
            "static inline void zc_error(const char* mensaje) {",
            "    fflush(stdout);",
            "    fprintf(stderr, \"\\nError en tiempo de ejecucion: %s\\n\", mensaje);",
            "    exit(1);",
            "}",
            "",
            "static inline void zc_verificar_objeto(const void* objeto, const char* metodo) {",
            "    if (objeto == NULL) {",
            "        fflush(stdout);",
            "        fprintf(stderr, \"\\nError en tiempo de ejecucion: se llamo a %s sobre un objeto nulo\\n\", metodo);",
            "        exit(1);",
            "    }",
            "}",
            "",
            "static inline void* zc_reservar(size_t tamanio) {",
            "    void* memoria = calloc(1, tamanio > 0 ? tamanio : 1);",
            "    if (memoria == NULL) {",
            "        zc_error(\"memoria insuficiente\");",
            "    }",
            "    return memoria;",
            "}",
            "",
            "static inline void* zc_arreglo_nivel(int nivel, int dimensiones, const int* tamanios, size_t tamanio_elemento) {",
            "    int n = tamanios[nivel];",
            "    if (n < 0) {",
            "        zc_error(\"tamanio de arreglo negativo\");",
            "    }",
            "    if (nivel == dimensiones - 1) {",
            "        return zc_reservar((size_t) n * tamanio_elemento);",
            "    }",
            "    void** filas = zc_reservar((size_t) n * sizeof(void*));",
            "    for (int i = 0; i < n; i++) {",
            "        filas[i] = zc_arreglo_nivel(nivel + 1, dimensiones, tamanios, tamanio_elemento);",
            "    }",
            "    return filas;",
            "}",
            "",
            "/* zc_nuevo_arreglo(2, sizeof(int), 3, 4) reserva un int[3][4] (todo en 0). */",
            "static inline void* zc_nuevo_arreglo(int dimensiones, size_t tamanio_elemento, ...) {",
            "    int tamanios[16];",
            "    va_list args;",
            "    va_start(args, tamanio_elemento);",
            "    for (int i = 0; i < dimensiones && i < 16; i++) {",
            "        tamanios[i] = va_arg(args, int);",
            "    }",
            "    va_end(args);",
            "    return zc_arreglo_nivel(0, dimensiones, tamanios, tamanio_elemento);",
            "}",
            "",
            "static inline char* zc_concatenar(const char* a, const char* b) {",
            "    if (a == NULL) a = \"null\";",
            "    if (b == NULL) b = \"null\";",
            "    size_t la = strlen(a), lb = strlen(b);",
            "    char* resultado = zc_reservar(la + lb + 1);",
            "    memcpy(resultado, a, la);",
            "    memcpy(resultado + la, b, lb + 1);",
            "    return resultado;",
            "}",
            "",
            "static inline char* zc_entero_a_cadena(int valor) {",
            "    char texto[32];",
            "    snprintf(texto, sizeof texto, \"%d\", valor);",
            "    return zc_concatenar(texto, \"\");",
            "}",
            "",
            "/* 2.5 -> \"2.5\", 3.0 -> \"3.0\" (como en el lenguaje fuente, no \"2.500000\") */",
            "static inline char* zc_decimal_a_cadena(double valor) {",
            "    char texto[64];",
            "    snprintf(texto, sizeof texto, \"%.15g\", valor);",
            "    if (strpbrk(texto, \".eEni\") == NULL) {",
            "        strcat(texto, \".0\");",
            "    }",
            "    return zc_concatenar(texto, \"\");",
            "}",
            "",
            "static inline char* zc_caracter_a_cadena(char valor) {",
            "    char texto[2] = {valor, '\\0'};",
            "    return zc_concatenar(texto, \"\");",
            "}",
            "",
            "static inline int zc_cadenas_iguales(const char* a, const char* b) {",
            "    if (a == b) return 1;",
            "    if (a == NULL || b == NULL) return 0;",
            "    return strcmp(a, b) == 0;",
            "}",
            "",
            "/* Lee una linea completa (sin el salto de linea). Nunca devuelve NULL. */",
            "static inline char* zc_leer_linea(void) {",
            "    fflush(stdout);",
            "    size_t capacidad = 64, largo = 0;",
            "    char* linea = zc_reservar(capacidad);",
            "    int c;",
            "    while ((c = getchar()) != EOF && c != '\\n') {",
            "        if (largo + 1 >= capacidad) {",
            "            capacidad *= 2;",
            "            linea = realloc(linea, capacidad);",
            "            if (linea == NULL) zc_error(\"memoria insuficiente\");",
            "        }",
            "        linea[largo++] = (char) c;",
            "    }",
            "    if (largo > 0 && linea[largo - 1] == '\\r') largo--;",
            "    linea[largo] = '\\0';",
            "    return linea;",
            "}",
            "",
            "static inline char* zc_leer_cadena(void) { return zc_leer_linea(); }",
            "static inline int zc_leer_entero(void) { return (int) strtol(zc_leer_linea(), NULL, 10); }",
            "static inline double zc_leer_decimal(void) { return strtod(zc_leer_linea(), NULL); }",
            "static inline char zc_leer_caracter(void) { return zc_leer_linea()[0]; }",
            "",
            "/* ================================================================== */",
            "");

    /** Traduce un Tipo del compilador a su declaracion equivalente en C. */
    public static String tipoC(Tipo tipo) {
        if (tipo == null) {
            return "int";
        }
        StringBuilder sb = new StringBuilder(tipoBaseC(tipo));
        for (int i = 0; i < tipo.getProfundidadArreglo(); i++) {
            sb.append('*'); // arreglo -> puntero (se reserva con zc_nuevo_arreglo)
        }
        return sb.toString();
    }

    private static String tipoBaseC(Tipo tipo) {
        if (tipo.getPrimitivo() == null) {
            return "int";
        }
        switch (tipo.getPrimitivo()) {
            case ESTRUCTURA: return tipo.getNombreEstructura() + "*"; // los objetos/estructuras viven en heap
            case DECIMAL:    return "double";
            case CARACTER:   return "char";
            case CADENA:     return "char*";
            case VACIO:      return "void";
            case NULO:       return "void*";
            default:         return "int";    // ENTERO, BOOLEANO (y ERROR como comodin)
        }
    }

    /**
     * Formato de printf para un valor de este tipo. Para DECIMAL es
     * FORMATO_DECIMAL: ImprimirCuarteta lo imprime con zc_decimal_a_cadena
     * (2.5 y no 2.500000).
     */
    public static final String FORMATO_DECIMAL = "%f";

    public static String formatoImpresion(Tipo tipo) {
        if (tipo == null || tipo.esArreglo() || tipo.getPrimitivo() == null) {
            return "%d";
        }
        switch (tipo.getPrimitivo()) {
            case DECIMAL:  return FORMATO_DECIMAL;
            case CARACTER: return "%c";
            case CADENA:   return "%s";
            default:       return "%d";
        }
    }

    /** Funcion del runtime que lee una linea y la convierte a este tipo. */
    public static String funcionLectura(Tipo tipo) {
        if (tipo == null || tipo.esArreglo() || tipo.getPrimitivo() == null) {
            return "zc_leer_entero";
        }
        switch (tipo.getPrimitivo()) {
            case DECIMAL:  return "zc_leer_decimal";
            case CARACTER: return "zc_leer_caracter";
            case CADENA:   return "zc_leer_cadena";
            default:       return "zc_leer_entero";
        }
    }

    /** Funcion que convierte un valor de este tipo a char*, o null si ya es cadena. */
    public static String conversionACadena(Tipo tipo) {
        if (tipo == null || tipo.esArreglo() || tipo.getPrimitivo() == null) {
            return "zc_entero_a_cadena";
        }
        switch (tipo.getPrimitivo()) {
            case CADENA:   return null;
            case NULO:     return null;
            case DECIMAL:  return "zc_decimal_a_cadena";
            case CARACTER: return "zc_caracter_a_cadena";
            default:       return "zc_entero_a_cadena";
        }
    }
}
