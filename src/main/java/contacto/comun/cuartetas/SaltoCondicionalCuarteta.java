package contacto.comun.cuartetas;

import contacto.comun.cuartetas.acceso.Lugar;

/**
 * if (!condicion) goto etiquetaDestino   (saltaSiFalso = true)
 * if (condicion)  goto etiquetaDestino   (saltaSiFalso = false)
 */
public class SaltoCondicionalCuarteta extends Cuarteta {

    private final Lugar condicion;
    private final boolean saltaSiFalso;
    private final String etiquetaDestino;

    public SaltoCondicionalCuarteta(int numero, Lugar condicion, boolean saltaSiFalso, String etiquetaDestino) {
        super(numero);
        this.condicion = condicion;
        this.saltaSiFalso = saltaSiFalso;
        this.etiquetaDestino = etiquetaDestino;
    }

    public Lugar getCondicion() {
        return condicion;
    }

    public boolean isSaltaSiFalso() {
        return saltaSiFalso;
    }

    public String getEtiquetaDestino() {
        return etiquetaDestino;
    }

    @Override
    public void generarC(StringBuilder codigo) {
        codigo.append("if (");
        if (saltaSiFalso) {
            codigo.append('!').append('(');
            condicion.generarC(codigo);
            codigo.append(')');
        } else {
            condicion.generarC(codigo);
        }
        codigo.append(") goto ").append(etiquetaDestino).append(";\n");
    }

    @Override
    public String toString() {
        String palabra = saltaSiFalso ? "if_false " : "if_true ";
        return getNumero() + ": " + palabra + condicion + " goto " + etiquetaDestino;
    }
}
