package contacto.comun.cuartetas;

/** destino = origen  (copia directa, sin operador). */
public class AsignacionCuarteta extends Cuarteta {

    private final String destino;
    private final String origen;

    public AsignacionCuarteta(int numero, String destino, String origen) {
        super(numero);
        this.destino = destino;
        this.origen = origen;
    }

    public String getDestino() {
        return destino;
    }

    public String getOrigen() {
        return origen;
    }

    @Override
    public String toString() {
        return getNumero() + ": " + destino + " = " + origen;
    }
}
