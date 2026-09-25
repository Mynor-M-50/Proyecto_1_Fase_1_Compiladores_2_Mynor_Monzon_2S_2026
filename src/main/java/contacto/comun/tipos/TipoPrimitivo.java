package contacto.comun.tipos;

/**
 * Categorias de tipo primitivo, CANONICAS y comunes a los tres
 * lenguajes (Y?, Zetariano, Pig Latin). Cada lenguaje tiene sus propias
 * palabras reservadas para lo mismo:
 *
 *   ENTERO    ->  entero (Y?)     int (Zetariano)     numerus (Pig Latin)
 *   DECIMAL   ->  flotante        double              decimalis
 *   CARACTER  ->  caracter        char                littera
 *   CADENA    ->  cadena          String              textum
 *   BOOLEANO  ->  bool            boolean             bool / verum-falsus
 *
 * El listener semantico de cada lenguaje traduce su propio token
 * concreto a uno de estos valores; de ahi para arriba (TablaTipos,
 * comparaciones, mensajes de error) todo es compartido.
 */
public enum TipoPrimitivo {
    ENTERO,
    DECIMAL,
    CARACTER,
    CADENA,
    BOOLEANO,
    ESTRUCTURA,   // clase, struct u objeto; el nombre concreto va en Tipo.nombreEstructura
    NULO,         // el literal "null" / valor nulo: asignable a cualquier ESTRUCTURA
    VACIO,        // "void" / una funcion o metodo sin retorno
    ERROR;        // comodin: algo ya fallo (o es externo/no verificable), no arrastrar mas errores

    public boolean esNumerico() {
        return this == ENTERO || this == DECIMAL;
    }
}
