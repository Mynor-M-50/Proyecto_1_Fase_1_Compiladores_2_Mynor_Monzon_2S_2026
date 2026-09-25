package contacto.comun.cuartetas;

/**
 * if_false condicion goto etiquetaDestino   (saltaSiFalso = true)
 * if_true  condicion goto etiquetaDestino   (saltaSiFalso = false)
 */
public class SaltoCondicionalCuarteta extends Cuarteta {

    private final String condicion;
    private final boolean saltaSiFalso;
    private final String etiquetaDestino;

    public SaltoCondicionalCuarteta(int numero, String condicion, boolean saltaSiFalso, String etiquetaDestino) {
        super(numero);
        this.condicion = condicion;
        this.saltaSiFalso = saltaSiFalso;
        this.etiquetaDestino = etiquetaDestino;
    }

    public String getCondicion() {
        return condicion;
    }

    public boolean isSaltaSiFalso() {
        return saltaSiFalso;
    }

    public String getEtiquetaDestino() {
        return etiquetaDestino;
    }

    @Override
    public String toString() {
        String palabra = saltaSiFalso ? "if_false " : "if_true ";
        return getNumero() + ": " + palabra + condicion + " goto " + etiquetaDestino;
    }
}
