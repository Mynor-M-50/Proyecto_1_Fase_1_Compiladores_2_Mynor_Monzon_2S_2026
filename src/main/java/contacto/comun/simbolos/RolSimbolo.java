package contacto.comun.simbolos;

/**
 * Que tipo de "cosa" representa un simbolo dentro de la tabla. No todos
 * los lenguajes usan todos los valores (p.ej. Y? no tiene CONSTRUCTOR),
 * pero al compartir el enum evitamos tener tres versiones casi iguales.
 */
public enum RolSimbolo {
    VARIABLE,
    PARAMETRO,
    FUNCION,
    METODO,
    CONSTRUCTOR,
    CLASE,
    ESTRUCTURA,
    ARREGLO
}
