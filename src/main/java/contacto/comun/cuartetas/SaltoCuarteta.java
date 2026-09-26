package contacto.comun.cuartetas;

/** goto etiquetaDestino  (salto incondicional). */
public class SaltoCuarteta extends Cuarteta {

    private final String etiquetaDestino;

    public SaltoCuarteta(int numero, String etiquetaDestino) {
        super(numero);
        this.etiquetaDestino = etiquetaDestino;
    }

    public String getEtiquetaDestino() {
        return etiquetaDestino;
    }

    @Override
    public void generarC(StringBuilder codigo) {
        codigo.append("goto ").append(etiquetaDestino).append(";\n");
    }

    @Override
    public String toString() {
        return getNumero() + ": goto " + etiquetaDestino;
    }
}
