package contacto.comun.tipos;

/**
 * Reglas de compatibilidad e inferencia de tipos. Concentra en un solo
 * lugar las decisiones de tipado para que los analizadores semanticos
 * de los 3 lenguajes no las tengan dispersas ni repetidas (adaptado de
 * TablaTipos.java de Codex Latinus, Practica 1).
 *
 * Reglas generales (las tres gramaticas piden "tomar en cuenta la
 * validacion implicita de tipos"):
 *   - entero se ensancha a decimal automaticamente (decimal NO se
 *     angosta a entero).
 *   - cadena + cualquier cosa = cadena (concatenacion, como en los
 *     ejemplos de los 3 lenguajes: "texto" + 100 + "texto").
 *   - null es asignable a cualquier tipo ESTRUCTURA.
 *   - ERROR nunca genera un mensaje nuevo (para no arrastrar errores).
 *
 * TODO para cuando haya tiempo: char -> int implicito (como en Java),
 * y reglas de asignacion mas finas entre estructuras compatibles.
 */
public final class TablaTipos {

    private TablaTipos() {
        // Clase de utilidades, no se instancia
    }

    // ---- Operaciones binarias --------------------------------------------------------

    public static Tipo resultadoBinario(Tipo izquierdo, Operador operador, Tipo derecho) {
        if (izquierdo == null || derecho == null || operador == null) {
            return Tipo.error();
        }
        if (izquierdo.esError() || derecho.esError()) {
            return Tipo.error();
        }

        switch (operador) {
            case SUMA:
                return resultadoSuma(izquierdo, derecho);
            case RESTA:
            case MULTIPLICACION:
            case DIVISION:
            case MODULO:
                return resultadoAritmetica(izquierdo, derecho);
            case MENOR:
            case MAYOR:
            case MENOR_IGUAL:
            case MAYOR_IGUAL:
                return resultadoRelacional(izquierdo, derecho);
            case IGUAL:
            case DIFERENTE:
                return resultadoIgualdad(izquierdo, derecho);
            case AND:
            case OR:
                return resultadoLogico(izquierdo, derecho);
            default:
                return Tipo.error();
        }
    }

    // "+" es especial: si cualquiera de los dos lados es cadena, el
    // resultado es cadena (concatenacion). Si no, es aritmetica normal.
    private static Tipo resultadoSuma(Tipo izquierdo, Tipo derecho) {
        if (esCadenaEscalar(izquierdo) || esCadenaEscalar(derecho)) {
            return Tipo.cadena();
        }
        return resultadoAritmetica(izquierdo, derecho);
    }

    private static boolean esCadenaEscalar(Tipo tipo) {
        return tipo.esEscalar() && tipo.getPrimitivo() == TipoPrimitivo.CADENA;
    }

    private static Tipo resultadoAritmetica(Tipo izquierdo, Tipo derecho) {
        if (!izquierdo.esNumerico() || !derecho.esNumerico()) {
            return Tipo.error();
        }
        boolean hayDecimal = izquierdo.getPrimitivo() == TipoPrimitivo.DECIMAL
                || derecho.getPrimitivo() == TipoPrimitivo.DECIMAL;
        return hayDecimal ? Tipo.decimal() : Tipo.entero();
    }

    private static Tipo resultadoRelacional(Tipo izquierdo, Tipo derecho) {
        if (!izquierdo.esNumerico() || !derecho.esNumerico()) {
            return Tipo.error();
        }
        return Tipo.booleano();
    }

    private static Tipo resultadoIgualdad(Tipo izquierdo, Tipo derecho) {
        // null == objeto / objeto == null: siempre valido
        if (izquierdo.esNulo() || derecho.esNulo()) {
            if (izquierdo.esNulo() && derecho.esNulo()) {
                return Tipo.booleano();
            }
            Tipo otro = izquierdo.esNulo() ? derecho : izquierdo;
            return otro.esEstructura() ? Tipo.booleano() : Tipo.error();
        }
        if (izquierdo.esNumerico() && derecho.esNumerico()) {
            return Tipo.booleano();
        }
        if (izquierdo.mismoTipoQue(derecho)) {
            return Tipo.booleano();
        }
        return Tipo.error();
    }

    private static Tipo resultadoLogico(Tipo izquierdo, Tipo derecho) {
        if (!izquierdo.esBooleano() || !derecho.esBooleano()) {
            return Tipo.error();
        }
        return Tipo.booleano();
    }

    // ---- Operaciones unarias --------------------------------------------------------

    public static Tipo resultadoUnario(Operador operador, Tipo operando) {
        if (operador == null || operando == null || operando.esError()) {
            return Tipo.error();
        }
        if (operador == Operador.MENOS_UNARIO) {
            return operando.esNumerico() ? operando : Tipo.error();
        }
        if (operador == Operador.NEGACION_LOGICA) {
            return operando.esBooleano() ? Tipo.booleano() : Tipo.error();
        }
        return Tipo.error();
    }

    // ---- Asignacion / paso de parametros ---------------------------------------------

    public static boolean esAsignable(Tipo destino, Tipo origen) {
        if (destino == null || origen == null) {
            return false;
        }
        if (destino.esError() || origen.esError()) {
            return true; // no arrastrar errores
        }
        if (destino.mismoTipoQue(origen)) {
            return true;
        }
        // ensanchamiento entero -> decimal (nunca al reves)
        if (!destino.esArreglo() && !origen.esArreglo()
                && destino.getPrimitivo() == TipoPrimitivo.DECIMAL
                && origen.getPrimitivo() == TipoPrimitivo.ENTERO) {
            return true;
        }
        // null asignable a cualquier estructura/objeto
        if (destino.esEstructura() && origen.esNulo()) {
            return true;
        }
        return false;
    }

    public static boolean esCondicionValida(Tipo tipo) {
        return tipo != null && (tipo.esError() || tipo.esBooleano());
    }

    // ---- Mensajes de error -------------------------------------------------------------

    public static String mensajeBinario(Tipo izquierdo, Operador operador, Tipo derecho) {
        return "El operador '" + operador + "' no se puede aplicar entre "
                + izquierdo + " y " + derecho;
    }

    public static String mensajeUnario(Operador operador, Tipo operando) {
        return "El operador '" + operador + "' no se puede aplicar a " + operando;
    }

    public static String mensajeAsignacion(Tipo destino, Tipo origen) {
        return "No se puede asignar un valor de tipo " + origen
                + " a una variable de tipo " + destino;
    }
}
