package contacto.comun.cuartetas;

import contacto.comun.cuartetas.acceso.Lugar;

/** destino = origen  (copia directa, sin operador). */
public class AsignacionCuarteta extends Cuarteta {

    private final Lugar destino;
    private final Lugar origen;

    public AsignacionCuarteta(int numero, Lugar destino, Lugar origen) {
        super(numero);
        this.destino = destino;
        this.origen = origen;
    }

    public Lugar getDestino() {
        return destino;
    }

    public Lugar getOrigen() {
        return origen;
    }

    @Override
    public void generarC(StringBuilder codigo) {
        destino.generarC(codigo);
        codigo.append(" = ");
        origen.generarC(codigo);
        codigo.append(";\n");
    }

    @Override
    public String toString() {
        return getNumero() + ": " + destino + " = " + origen;
    }
}
