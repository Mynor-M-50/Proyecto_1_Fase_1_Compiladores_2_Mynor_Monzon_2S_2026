package contacto.comun.cuartetas;

/** read destino */
public class LeerCuarteta extends Cuarteta {

    private final String destino;

    public LeerCuarteta(int numero, String destino) {
        super(numero);
        this.destino = destino;
    }

    public String getDestino() {
        return destino;
    }

    @Override
    public String toString() {
        return getNumero() + ": read " + destino;
    }
}
