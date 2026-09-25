package contacto.comun.tipos;

import java.util.Objects;

/**
 * Representa el tipo completo de una expresion, variable, campo o
 * retorno, para cualquiera de los tres lenguajes.
 *
 * A diferencia de la version de Codex Latinus (Practica 1), aqui el
 * arreglo se modela como PROFUNDIDAD (rank), no como una sola bandera
 * booleana + tamanio: int[][][] es profundidadArreglo=3. Esto hace
 * falta porque los tres lenguajes permiten arreglos multidimensionales
 * con inicializacion anidada ({{1,2},{3,4}}), confirmado por la
 * auxiliar en el foro del curso. No se guarda el TAMANIO de cada
 * dimension (eso se resuelve en tiempo de ejecucion / al generar
 * cuartetas, no es parte del tipo en si).
 *
 * Inmutable a proposito: cada operacion devuelve un Tipo nuevo.
 */
public final class Tipo {

    private final TipoPrimitivo primitivo;
    private final String nombreEstructura;
    private final int profundidadArreglo; // 0 = no es arreglo; N = arreglo de N dimensiones

    private Tipo(TipoPrimitivo primitivo, String nombreEstructura, int profundidadArreglo) {
        this.primitivo = primitivo;
        this.nombreEstructura = nombreEstructura;
        this.profundidadArreglo = profundidadArreglo;
    }

    // ---- fabricas ----

    public static Tipo de(TipoPrimitivo primitivo) {
        return new Tipo(primitivo, null, 0);
    }

    public static Tipo entero()   { return de(TipoPrimitivo.ENTERO); }
    public static Tipo decimal()  { return de(TipoPrimitivo.DECIMAL); }
    public static Tipo caracter() { return de(TipoPrimitivo.CARACTER); }
    public static Tipo cadena()   { return de(TipoPrimitivo.CADENA); }
    public static Tipo booleano() { return de(TipoPrimitivo.BOOLEANO); }
    public static Tipo nulo()     { return de(TipoPrimitivo.NULO); }
    public static Tipo vacio()    { return de(TipoPrimitivo.VACIO); }
    public static Tipo error()    { return de(TipoPrimitivo.ERROR); }

    public static Tipo estructura(String nombre) {
        return new Tipo(TipoPrimitivo.ESTRUCTURA, nombre, 0);
    }

    /** Envuelve en un nivel mas de arreglo: arregloDe(arregloDe(entero())) es int[][]. */
    public static Tipo arregloDe(Tipo base) {
        return new Tipo(base.primitivo, base.nombreEstructura, base.profundidadArreglo + 1);
    }

    /** Envuelve en "nivelesExtra" niveles de arreglo de una vez (para new int[3][3]). */
    public static Tipo arregloDe(Tipo base, int nivelesExtra) {
        Tipo resultado = base;
        for (int i = 0; i < nivelesExtra; i++) {
            resultado = arregloDe(resultado);
        }
        return resultado;
    }

    // ---- consultas ----

    public TipoPrimitivo getPrimitivo() {
        return primitivo;
    }

    public String getNombreEstructura() {
        return nombreEstructura;
    }

    public boolean esArreglo() {
        return profundidadArreglo > 0;
    }

    public int getProfundidadArreglo() {
        return profundidadArreglo;
    }

    public boolean esEstructura() {
        return primitivo == TipoPrimitivo.ESTRUCTURA;
    }

    public boolean esNulo() {
        return primitivo == TipoPrimitivo.NULO;
    }

    public boolean esError() {
        return primitivo == TipoPrimitivo.ERROR;
    }

    public boolean esVacio() {
        return primitivo == TipoPrimitivo.VACIO;
    }

    public boolean esBooleano() {
        return !esArreglo() && primitivo == TipoPrimitivo.BOOLEANO;
    }

    public boolean esNumerico() {
        return !esArreglo() && primitivo.esNumerico();
    }

    public boolean esEscalar() {
        return !esArreglo();
    }

    /** Un nivel hacia adentro: int[][] -> int[]; int[] -> int; int -> int. */
    public Tipo tipoElemento() {
        if (!esArreglo()) {
            return this;
        }
        return new Tipo(primitivo, nombreEstructura, profundidadArreglo - 1);
    }

    public boolean mismoTipoQue(Tipo otro) {
        if (otro == null) {
            return false;
        }
        return primitivo == otro.primitivo
                && profundidadArreglo == otro.profundidadArreglo
                && Objects.equals(nombreEstructura, otro.nombreEstructura);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Tipo)) {
            return false;
        }
        return mismoTipoQue((Tipo) obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primitivo, nombreEstructura, profundidadArreglo);
    }

    @Override
    public String toString() {
        String base = esEstructura() ? nombreEstructura : primitivo.name().toLowerCase();
        StringBuilder sb = new StringBuilder(base);
        for (int i = 0; i < profundidadArreglo; i++) {
            sb.append("[]");
        }
        return sb.toString();
    }
}
