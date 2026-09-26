// Generado automaticamente a partir de test-programs/pila/main.pig
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>

/* ===================== runtime del compilador ===================== */

static inline void zc_error(const char* mensaje) {
    fflush(stdout);
    fprintf(stderr, "\nError en tiempo de ejecucion: %s\n", mensaje);
    exit(1);
}

static inline void zc_verificar_objeto(const void* objeto, const char* metodo) {
    if (objeto == NULL) {
        fflush(stdout);
        fprintf(stderr, "\nError en tiempo de ejecucion: se llamo a %s sobre un objeto nulo\n", metodo);
        exit(1);
    }
}

static inline void* zc_reservar(size_t tamanio) {
    void* memoria = calloc(1, tamanio > 0 ? tamanio : 1);
    if (memoria == NULL) {
        zc_error("memoria insuficiente");
    }
    return memoria;
}

static inline void* zc_arreglo_nivel(int nivel, int dimensiones, const int* tamanios, size_t tamanio_elemento) {
    int n = tamanios[nivel];
    if (n < 0) {
        zc_error("tamanio de arreglo negativo");
    }
    if (nivel == dimensiones - 1) {
        return zc_reservar((size_t) n * tamanio_elemento);
    }
    void** filas = zc_reservar((size_t) n * sizeof(void*));
    for (int i = 0; i < n; i++) {
        filas[i] = zc_arreglo_nivel(nivel + 1, dimensiones, tamanios, tamanio_elemento);
    }
    return filas;
}

/* zc_nuevo_arreglo(2, sizeof(int), 3, 4) reserva un int[3][4] (todo en 0). */
static inline void* zc_nuevo_arreglo(int dimensiones, size_t tamanio_elemento, ...) {
    int tamanios[16];
    va_list args;
    va_start(args, tamanio_elemento);
    for (int i = 0; i < dimensiones && i < 16; i++) {
        tamanios[i] = va_arg(args, int);
    }
    va_end(args);
    return zc_arreglo_nivel(0, dimensiones, tamanios, tamanio_elemento);
}

static inline char* zc_concatenar(const char* a, const char* b) {
    if (a == NULL) a = "null";
    if (b == NULL) b = "null";
    size_t la = strlen(a), lb = strlen(b);
    char* resultado = zc_reservar(la + lb + 1);
    memcpy(resultado, a, la);
    memcpy(resultado + la, b, lb + 1);
    return resultado;
}

static inline char* zc_entero_a_cadena(int valor) {
    char texto[32];
    snprintf(texto, sizeof texto, "%d", valor);
    return zc_concatenar(texto, "");
}

/* 2.5 -> "2.5", 3.0 -> "3.0" (como en el lenguaje fuente, no "2.500000") */
static inline char* zc_decimal_a_cadena(double valor) {
    char texto[64];
    snprintf(texto, sizeof texto, "%.15g", valor);
    if (strpbrk(texto, ".eEni") == NULL) {
        strcat(texto, ".0");
    }
    return zc_concatenar(texto, "");
}

static inline char* zc_caracter_a_cadena(char valor) {
    char texto[2] = {valor, '\0'};
    return zc_concatenar(texto, "");
}

static inline int zc_cadenas_iguales(const char* a, const char* b) {
    if (a == b) return 1;
    if (a == NULL || b == NULL) return 0;
    return strcmp(a, b) == 0;
}

/* Lee una linea completa (sin el salto de linea). Nunca devuelve NULL. */
static inline char* zc_leer_linea(void) {
    fflush(stdout);
    size_t capacidad = 64, largo = 0;
    char* linea = zc_reservar(capacidad);
    int c;
    while ((c = getchar()) != EOF && c != '\n') {
        if (largo + 1 >= capacidad) {
            capacidad *= 2;
            linea = realloc(linea, capacidad);
            if (linea == NULL) zc_error("memoria insuficiente");
        }
        linea[largo++] = (char) c;
    }
    if (largo > 0 && linea[largo - 1] == '\r') largo--;
    linea[largo] = '\0';
    return linea;
}

static inline char* zc_leer_cadena(void) { return zc_leer_linea(); }
static inline int zc_leer_entero(void) { return (int) strtol(zc_leer_linea(), NULL, 10); }
static inline double zc_leer_decimal(void) { return strtod(zc_leer_linea(), NULL); }
static inline char zc_leer_caracter(void) { return zc_leer_linea()[0]; }

/* ================================================================== */

typedef struct Nodo Nodo;
typedef struct Pila Pila;

struct Nodo {
    int dato;
    Nodo* siguiente;
};

struct Pila {
    Nodo* cima;
    int tamanio;
};

Nodo* new_Nodo(int dato1);
int Nodo_getDato(Nodo* this);
void Nodo_setDato(Nodo* this, int dato1);
Nodo* Nodo_getSiguiente(Nodo* this);
void Nodo_setSiguiente(Nodo* this, Nodo* siguiente1);
Pila* new_Pila(void);
void Pila_apilar(Pila* this, int dato);
int Pila_desapilar(Pila* this);
int Pila_obtenerCima(Pila* this);
int Pila_estaVacia(Pila* this);
int Pila_obtenerTamanio(Pila* this);
char* Pila_toString(Pila* this);
void imprimirBienvenida(void);

Nodo* new_Nodo(int dato1) {
    Nodo* this = zc_reservar(sizeof(Nodo));

    // inicio_Nodo_constructor:
    this->dato = dato1;
    this->siguiente = NULL;
    return this;
}

int Nodo_getDato(Nodo* this) {
    zc_verificar_objeto(this, "Nodo.getDato");

    // inicio_getDato:
    return this->dato;
}

void Nodo_setDato(Nodo* this, int dato1) {
    zc_verificar_objeto(this, "Nodo.setDato");

    // inicio_setDato:
    this->dato = dato1;
}

Nodo* Nodo_getSiguiente(Nodo* this) {
    zc_verificar_objeto(this, "Nodo.getSiguiente");

    // inicio_getSiguiente:
    return this->siguiente;
}

void Nodo_setSiguiente(Nodo* this, Nodo* siguiente1) {
    zc_verificar_objeto(this, "Nodo.setSiguiente");

    // inicio_setSiguiente:
    this->siguiente = siguiente1;
}

Pila* new_Pila(void) {
    Pila* this = zc_reservar(sizeof(Pila));

    // inicio_Pila_constructor:
    this->cima = NULL;
    this->tamanio = 0;
    return this;
}

void Pila_apilar(Pila* this, int dato) {
    zc_verificar_objeto(this, "Pila.apilar");
    Nodo* nuevo = 0;
    Nodo* t0 = 0;

    // inicio_apilar:
    t0 = new_Nodo(dato);
    nuevo = t0;
    Nodo_setSiguiente(nuevo, this->cima);
    this->cima = nuevo;
    this->tamanio = this->tamanio + 1;
}

int Pila_desapilar(Pila* this) {
    zc_verificar_objeto(this, "Pila.desapilar");
    int dato = 0;
    int t1 = 0;
    Nodo* t2 = 0;

    // inicio_desapilar:
    t1 = Nodo_getDato(this->cima);
    dato = t1;
    t2 = Nodo_getSiguiente(this->cima);
    this->cima = t2;
    this->tamanio = this->tamanio - 1;
    return dato;
}

int Pila_obtenerCima(Pila* this) {
    zc_verificar_objeto(this, "Pila.obtenerCima");
    int t3 = 0;
    int t4 = 0;
    int t5 = 0;

    // inicio_obtenerCima:
    t3 = Pila_estaVacia(this);
    if (!(t3)) goto L1;
    t4 = -1;
    return t4;
    goto L0;
    L1:;
    L0:;
    t5 = Nodo_getDato(this->cima);
    return t5;
}

int Pila_estaVacia(Pila* this) {
    zc_verificar_objeto(this, "Pila.estaVacia");
    int t6 = 0;

    // inicio_estaVacia:
    t6 = this->cima == NULL;
    return t6;
}

int Pila_obtenerTamanio(Pila* this) {
    zc_verificar_objeto(this, "Pila.obtenerTamanio");

    // inicio_obtenerTamanio:
    return this->tamanio;
}

char* Pila_toString(Pila* this) {
    zc_verificar_objeto(this, "Pila.toString");
    char* resultado = 0;
    Nodo* actual = 0;
    int t7 = 0;
    int t8 = 0;
    char* t9 = 0;
    Nodo* t10 = 0;
    int t11 = 0;
    char* t12 = 0;
    Nodo* t13 = 0;

    // inicio_toString:
    resultado = "Pila [Cima -> Base]: ";
    actual = this->cima;
    L2:;
    t7 = actual != NULL;
    if (!(t7)) goto L3;
    t8 = Nodo_getDato(actual);
    t9 = zc_concatenar(resultado, zc_entero_a_cadena(t8));
    resultado = t9;
    t10 = Nodo_getSiguiente(actual);
    t11 = t10 != NULL;
    if (!(t11)) goto L5;
    t12 = zc_concatenar(resultado, ", ");
    resultado = t12;
    goto L4;
    L5:;
    L4:;
    t13 = Nodo_getSiguiente(actual);
    actual = t13;
    goto L2;
    L3:;
    return resultado;
}

void imprimirBienvenida(void) {

    // inicio_imprimirBienvenida:
    printf("%s\n", "------------------------------------------");
    printf("%s\n", "Este es mi primer programa a bajo nivel :D");
    printf("%s\n", "With <3 by IGriega");
    printf("%s\n", "------------------------------------------");
}

int main(void) {
    Pila* pila = 0;
    int opcion = 0;
    int lectura = 0;
    int t0 = 0;
    int t1 = 0;
    int t2 = 0;
    int t3 = 0;
    int t4 = 0;
    int t5 = 0;
    char* t6 = 0;
    int t7 = 0;

    pila = new_Pila();
    t0 = -1;
    opcion = t0;
    lectura = 0;
    // inicio_programa:
    imprimirBienvenida();
    L0:;
    t1 = opcion != 4;
    if (!(t1)) goto L1;
    printf("%s", "-----------------------------------------------");
    printf("%s", "Ingresa la accion: \n");
    printf("%s", "1. Ingresar en pila \n");
    printf("%s", "2. Sacar de pila \n");
    printf("%s", "3. Imprimir pila \n");
    printf("%s", "4. Salir \n");
    printf("%s", "-----------------------------------------------");
    opcion = zc_leer_entero();
    t2 = opcion == 1;
    if (!(t2)) goto L3;
    printf("%s", "Ingresa el numero: \n");
    lectura = zc_leer_entero();
    Pila_apilar(pila, lectura);
    goto L2;
    L3:;
    t3 = opcion == 2;
    if (!(t3)) goto L4;
    t4 = Pila_desapilar(pila);
    lectura = t4;
    printf("%s", "Elemento desapilado: ");
    printf("%d", lectura);
    goto L2;
    L4:;
    t5 = opcion == 3;
    if (!(t5)) goto L5;
    t6 = Pila_toString(pila);
    printf("%s", t6);
    goto L2;
    L5:;
    t7 = opcion == 4;
    if (!(t7)) goto L6;
    printf("%s", "Fin del programa");
    goto L2;
    L6:;
    L2:;
    printf("%s", "Ingresa cualquier tecla para continuar ");
    lectura = zc_leer_entero();
    goto L0;
    L1:;
    return 0;
}

