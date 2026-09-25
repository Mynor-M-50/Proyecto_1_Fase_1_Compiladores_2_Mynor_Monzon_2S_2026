package contacto.comun.errores;

/**
 * Un error puntual (lexico, sintactico o semantico), con su ubicacion.
 * Inmutable a proposito: una vez creado un error no deberia cambiar.
 */
public final class ErrorCompilacion {

    private final TipoError tipo;
    private final String mensaje;
    private final int linea;
    private final int columna;

    public ErrorCompilacion(TipoError tipo, String mensaje, int linea, int columna) {
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.linea = linea;
        this.columna = columna;
    }

    public TipoError getTipo() {
        return tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    @Override
    public String toString() {
        return "[" + tipo + "] linea " + linea + ":" + columna + " - " + mensaje;
    }
}
