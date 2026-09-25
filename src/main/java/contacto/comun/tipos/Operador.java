package contacto.comun.tipos;

/**
 * Operadores canonicos. Cada lenguaje tiene su propio simbolo/palabra
 * concreta (p.ej. "&&" en Zetariano y Y?, pero Pig Latin lo comparte
 * tambien); el listener de cada uno traduce su token a uno de estos.
 */
public enum Operador {
    SUMA, RESTA, MULTIPLICACION, DIVISION, MODULO,
    MENOS_UNARIO, NEGACION_LOGICA,
    MENOR, MAYOR, MENOR_IGUAL, MAYOR_IGUAL,
    IGUAL, DIFERENTE,
    AND, OR;

    @Override
    public String toString() {
        switch (this) {
            case SUMA: return "+";
            case RESTA: return "-";
            case MULTIPLICACION: return "*";
            case DIVISION: return "/";
            case MODULO: return "%";
            case MENOS_UNARIO: return "-";
            case NEGACION_LOGICA: return "!";
            case MENOR: return "<";
            case MAYOR: return ">";
            case MENOR_IGUAL: return "<=";
            case MAYOR_IGUAL: return ">=";
            case IGUAL: return "==";
            case DIFERENTE: return "!=";
            case AND: return "&&";
            case OR: return "||";
            default: return name();
        }
    }
}
