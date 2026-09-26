package contacto.comun.errores;

/**
 * Un error puntual (lexico, sintactico o semantico), con su ubicacion
 * Y el archivo de origen -- necesario desde que el orquestador compila
 * varios archivos (.pig + sus imports .z/.y) en una sola pasada, donde
 * "linea 28" solo no alcanza para saber en cual archivo mirar.
 * Inmutable a proposito: una vez creado un error no deberia cambiar.
 */
public final class ErrorCompilacion {

    private final TipoError tipo;
    private final String mensaje;
    private final int linea;
    private final int columna;
    private final String archivo;

    public ErrorCompilacion(TipoError tipo, String mensaje, int linea, int columna, String archivo) {
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.linea = linea;
        this.columna = columna;
        this.archivo = archivo;
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

    public String getArchivo() {
        return archivo;
    }

    @Override
    public String toString() {
        String ubicacion = (archivo != null && !archivo.isEmpty()) ? archivo + ":" : "";
        return "[" + tipo + "] " + ubicacion + linea + ":" + columna + " - " + mensaje;
    }
}
